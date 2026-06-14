package net.focik.homeoffice.finance.infrastructure.csvimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeAiMatcher {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final int TIMEOUT_SECONDS = 5;

    @Value("${spring.ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${spring.ai.anthropic.chat.options.model:claude-haiku-4-5-20251001}")
    private String model;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public MatchResult match(String transactionText, List<Firm> allFirms, List<TransactionLabel> allLabels) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("spring.ai.anthropic.api-key not set, returning default match result");
                return new MatchResult(null, new ArrayList<>());
            }

            String prompt = buildPrompt(transactionText, allFirms, allLabels);
            String requestBody = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Claude API returned status {}: {}", response.statusCode(), response.body());
                return new MatchResult(null, new ArrayList<>());
            }

            return parseResponse(response.body());
        } catch (Exception e) {
            log.warn("Error matching firm via Claude API: {}", e.getMessage());
            return new MatchResult(null, new ArrayList<>());
        }
    }

    private String buildPrompt(String transactionText, List<Firm> allFirms, List<TransactionLabel> allLabels) {
        StringBuilder sb = new StringBuilder();
        sb.append("Przeanalizuj poniższy tekst transakcji bankowej i dopasuj do firmy.\n\n");
        sb.append("Tekst transakcji: ").append(transactionText).append("\n\n");

        sb.append("Dostępne firmy:\n");
        for (Firm firm : allFirms) {
            sb.append(String.format("- ID: %d, Nazwa: %s\n", firm.getId(), firm.getName()));
        }

        sb.append("\nDostępne labele:\n");
        for (TransactionLabel label : allLabels) {
            sb.append(String.format("- ID: %d, Nazwa: %s\n", label.getId(), label.getName()));
        }

        sb.append("\nZwróć odpowiedź w formacie JSON:\n");
        sb.append("{\n");
        sb.append("  \"firmId\": <ID firmy lub null>,\n");
        sb.append("  \"labelIds\": [<IDs etykiet>]\n");
        sb.append("}\n\n");
        sb.append("Jeśli transakcja nie pasuje do żadnej firmy, ustaw firmId na null.\n");
        sb.append("Zwróć TYLKO JSON, bez dodatkowego tekstu.");

        return sb.toString();
    }

    private String buildRequestBody(String prompt) throws Exception {
        return objectMapper.writeValueAsString(new AnthropicRequest(
                model,
                1024,
                new AnthropicMessage[]{
                        new AnthropicMessage("user", prompt)
                }
        ));
    }

    private MatchResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentArray = root.get("content");

            if (contentArray != null && contentArray.isArray() && !contentArray.isEmpty()) {
                String text = contentArray.get(0).get("text").asText();

                // Extract JSON from text (might be wrapped in markdown code blocks)
                String jsonText = extractJson(text);
                JsonNode jsonNode = objectMapper.readTree(jsonText);

                Integer firmId = jsonNode.get("firmId").isNull() ? null : jsonNode.get("firmId").asInt();
                List<Integer> labelIds = new ArrayList<>();

                JsonNode labelIdsNode = jsonNode.get("labelIds");
                if (labelIdsNode != null && labelIdsNode.isArray()) {
                    for (JsonNode labelId : labelIdsNode) {
                        labelIds.add(labelId.asInt());
                    }
                }

                return new MatchResult(firmId, labelIds);
            }
        } catch (Exception e) {
            log.debug("Error parsing Claude API response: {}", e.getMessage());
        }
        return new MatchResult(null, new ArrayList<>());
    }

    private String extractJson(String text) {
        // Try to extract JSON from markdown code blocks
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        // If no code blocks, assume text is JSON
        return text.trim();
    }

    public static class MatchResult {
        public final Integer firmId;
        public final List<Integer> labelIds;

        public MatchResult(Integer firmId, List<Integer> labelIds) {
            this.firmId = firmId;
            this.labelIds = labelIds;
        }
    }

    private static class AnthropicRequest {
        public String model;
        public int max_tokens;
        public AnthropicMessage[] messages;

        AnthropicRequest(String model, int max_tokens, AnthropicMessage[] messages) {
            this.model = model;
            this.max_tokens = max_tokens;
            this.messages = messages;
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
}
