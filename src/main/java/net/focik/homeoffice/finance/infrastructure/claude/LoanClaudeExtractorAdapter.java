package net.focik.homeoffice.finance.infrastructure.claude;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.loanproposal.LoanExtractionResult;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanExtractorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ekstrakcja danych kredytu z tekstu maila. Wzorzec identyczny jak {@code ClaudeAiMatcher}:
 * surowy HttpClient do api.anthropic.com, wymuszony json_schema w odpowiedzi - moduł finance nie
 * ma zależności od Spring AI ChatModel (to używane jest tylko w fileService), więc nie dokładamy
 * jej tutaj.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanClaudeExtractorAdapter implements LoanExtractorPort {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 3;
    private static final List<Integer> RETRYABLE_STATUS_CODES = List.of(429, 500, 502, 503, 529);
    private static final long[] RETRY_BACKOFF_MILLIS = {1000, 3000};

    @Value("${spring.ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${spring.ai.anthropic.chat.options.model:claude-haiku-4-5-20251001}")
    private String model;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String SYSTEM_PROMPT = """
            Jesteś ekspertem od analizowania polskich e-maili dotyczących kredytów/pożyczek
            ratalnych (np. PayPo, inne firmy/banki pożyczkowe/kredytowe). Zwróć TYLKO obiekt JSON,
            bez żadnego dodatkowego tekstu ani formatowania markdown.
            Jeśli e-mail NIE dotyczy przyznania/potwierdzenia kredytu lub pożyczki ratalnej,
            ustaw isLoanDocument na false i pozostałe pola na null.
            Kwoty: liczby dziesiętne z kropką (np. "123.45"), bez waluty.
            Daty: format YYYY-MM-DD.
            Wiadomość może być przekierowana z innej skrzynki (np. zawiera "Wiadomość przekazana
            dalej" / "Forwarded message" / "Od: ..." wewnątrz treści) - nagłówek From całego maila
            to wtedy adres osoby przekierowującej, NIE oryginalnego nadawcy. Jeśli rozpoznasz taki
            przekierowany blok nagłówków w treści, wyciągnij z niego oryginalny adres e-mail
            nadawcy do originalSenderEmail. Jeśli wiadomość nie jest przekierowana albo nie da się
            tego ustalić, zostaw null.
            """;

    private static final String USER_PROMPT_PREFIX = """
            Przeanalizuj poniższy e-mail i zwróć dane JSON:
            {
              "isLoanDocument": true/false,
              "originalSenderEmail": "adres e-mail oryginalnego nadawcy sprzed przekierowania, lub null",
              "bankOrCreditor": "nazwa banku lub firmy udzielającej kredytu/pożyczki (np. PayPo)",
              "merchantName": "nazwa sklepu/przedmiot zakupu sfinansowanego kredytem (np. 'GLOBAL-E.SHELLY EU' z 'zamówienie w sklepie X') - CO zostało kupione, nie KTO sfinansował; null, jeśli e-mail nie dotyczy konkretnego zakupu (np. zwykły kredyt gotówkowy)",
              "loanNumber": "numer umowy/kredytu lub null",
              "accountNumber": "numer rachunku bankowego do przelewu (np. z sekcji o tradycyjnym przelewie), bez spacji, lub null",
              "amount": "całkowita kwota kredytu",
              "installmentAmount": "kwota raty miesięcznej lub null",
              "numberOfInstallments": liczba rat (int) lub null,
              "firstPaymentDate": "YYYY-MM-DD lub null",
              "loanCost": "prowizja/koszt kredytu lub null",
              "otherInfo": "inne istotne informacje lub null"
            }

            Treść e-maila:
            """;

    @Override
    public LoanExtractionResult extract(String emailText) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("spring.ai.anthropic.api-key not set, cannot extract loan proposal");
            throw new IllegalStateException("Claude API key not configured");
        }
        if (emailText == null || emailText.isBlank()) {
            return LoanExtractionResult.builder().isLoanDocument(false).build();
        }

        String requestBody;
        try {
            requestBody = buildRequestBody(emailText);
        } catch (Exception e) {
            throw new IllegalStateException("Error building Claude API request: " + e.getMessage(), e);
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callOnce(requestBody);
            } catch (IOException e) {
                // Blad sieci/timeout - przejsciowy, warto ponowic (np. api.anthropic.com
                // chwilowo wolne/niedostepne). "request timed out" ladowalo tu przed retry.
                logAndMaybeRethrow(attempt, "network error/timeout: " + e.getMessage(), e);
            } catch (RetryableApiException e) {
                // 429/5xx ze strony Anthropic - rowniez przejsciowe.
                logAndMaybeRethrow(attempt, e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while calling Claude API", e);
            } catch (IllegalStateException e) {
                throw e; // 4xx nietrwały do naprawy przez retry (zly klucz, model, ksztalt odpowiedzi) - nie ponawiamy
            } catch (Exception e) {
                throw new IllegalStateException("Error calling Claude API: " + e.getMessage(), e);
            }

            sleepBeforeRetry(attempt);
        }
        throw new IllegalStateException("Error calling Claude API: exhausted " + MAX_ATTEMPTS + " attempts");
    }

    private void logAndMaybeRethrow(int attempt, String reason, Exception cause) {
        if (attempt >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Error calling Claude API after " + MAX_ATTEMPTS + " attempts: " + reason, cause);
        }
        log.warn("Claude API call failed (attempt {}/{}), retrying: {}", attempt, MAX_ATTEMPTS, reason);
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS[Math.min(attempt - 1, RETRY_BACKOFF_MILLIS.length - 1)]);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry Claude API call", e);
        }
    }

    private LoanExtractionResult callOnce(String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (RETRYABLE_STATUS_CODES.contains(response.statusCode())) {
            throw new RetryableApiException("Claude API returned status " + response.statusCode() + ": " + response.body());
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Claude API returned status " + response.statusCode() + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    /** Oznacza blad HTTP (429/5xx), ktory warto ponowic - w odroznieniu od IllegalStateException dla 4xx. */
    private static class RetryableApiException extends RuntimeException {
        RetryableApiException(String message) {
            super(message);
        }
    }

    private String buildRequestBody(String emailText) throws Exception {
        String prompt = USER_PROMPT_PREFIX + emailText;
        return objectMapper.writeValueAsString(new AnthropicRequest(
                model,
                1024,
                new AnthropicMessage[]{new AnthropicMessage("user", SYSTEM_PROMPT + "\n\n" + prompt)},
                new OutputConfig(new ResponseFormat("json_schema", buildResponseSchema()))
        ));
    }

    private Map<String, Object> buildResponseSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("isLoanDocument", Map.of("type", "boolean"));
        properties.put("originalSenderEmail", Map.of("type", List.of("string", "null")));
        properties.put("bankOrCreditor", Map.of("type", List.of("string", "null")));
        properties.put("merchantName", Map.of("type", List.of("string", "null")));
        properties.put("loanNumber", Map.of("type", List.of("string", "null")));
        properties.put("accountNumber", Map.of("type", List.of("string", "null")));
        properties.put("amount", Map.of("type", List.of("string", "null")));
        properties.put("installmentAmount", Map.of("type", List.of("string", "null")));
        properties.put("numberOfInstallments", Map.of("type", List.of("integer", "null")));
        properties.put("firstPaymentDate", Map.of("type", List.of("string", "null")));
        properties.put("loanCost", Map.of("type", List.of("string", "null")));
        properties.put("otherInfo", Map.of("type", List.of("string", "null")));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("isLoanDocument"));
        schema.put("additionalProperties", false);
        return schema;
    }

    private LoanExtractionResult parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentArray = root.get("content");

        if (contentArray == null || !contentArray.isArray() || contentArray.isEmpty()) {
            throw new IllegalStateException("Unexpected Claude API response shape: " + responseBody);
        }

        String text = contentArray.get(0).get("text").asText();
        JsonNode json = objectMapper.readTree(text);

        return LoanExtractionResult.builder()
                .isLoanDocument(json.path("isLoanDocument").asBoolean(false))
                .originalSenderEmail(textOrNull(json, "originalSenderEmail"))
                .bankOrCreditor(textOrNull(json, "bankOrCreditor"))
                .merchantName(textOrNull(json, "merchantName"))
                .loanNumber(textOrNull(json, "loanNumber"))
                .accountNumber(textOrNull(json, "accountNumber"))
                .amount(textOrNull(json, "amount"))
                .installmentAmount(textOrNull(json, "installmentAmount"))
                .numberOfInstallments(json.hasNonNull("numberOfInstallments") ? json.get("numberOfInstallments").asInt() : null)
                .firstPaymentDate(textOrNull(json, "firstPaymentDate"))
                .loanCost(textOrNull(json, "loanCost"))
                .otherInfo(textOrNull(json, "otherInfo"))
                .build();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private static class AnthropicRequest {
        public String model;
        public int max_tokens;
        public AnthropicMessage[] messages;
        public OutputConfig output_config;

        AnthropicRequest(String model, int max_tokens, AnthropicMessage[] messages, OutputConfig output_config) {
            this.model = model;
            this.max_tokens = max_tokens;
            this.messages = messages;
            this.output_config = output_config;
        }
    }

    private static class AnthropicMessage {
        public String role;
        public String content;

        AnthropicMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class OutputConfig {
        public ResponseFormat format;

        OutputConfig(ResponseFormat format) {
            this.format = format;
        }
    }

    private static class ResponseFormat {
        public String type;
        public Map<String, Object> schema;

        ResponseFormat(String type, Map<String, Object> schema) {
            this.type = type;
            this.schema = schema;
        }
    }
}
