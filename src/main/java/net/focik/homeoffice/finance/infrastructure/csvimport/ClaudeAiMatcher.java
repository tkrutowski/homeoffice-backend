package net.focik.homeoffice.finance.infrastructure.csvimport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Value("${claude.matcher.default-firm-id:38}")
    private int defaultFirmId;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public MatchResult match(String transactionText, List<Firm> allFirms, List<TransactionLabel> allLabels,
                              List<TransactionCategory> candidateCategories) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("spring.ai.anthropic.api-key not set, returning default match result");
                return new MatchResult(defaultFirmId, new ArrayList<>(), null);
            }

            String prompt = buildPrompt(transactionText, allFirms, allLabels, candidateCategories);
            String requestBody = buildRequestBody(prompt, candidateCategories);

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
                return new MatchResult(defaultFirmId, new ArrayList<>(), null);
            }

            return parseResponse(response.body());
        } catch (Exception e) {
            log.warn("Error matching firm via Claude API: {}", e.getMessage());
            return new MatchResult(defaultFirmId, new ArrayList<>(), null);
        }
    }

    private String buildPrompt(String transactionText, List<Firm> allFirms, List<TransactionLabel> allLabels,
                                List<TransactionCategory> candidateCategories) {
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

        sb.append("\nJeśli transakcja nie pasuje do żadnej firmy, ustaw firmId na ").append(defaultFirmId).append(".");

        if (!candidateCategories.isEmpty()) {
            sb.append("\n\nDostępne kategorie:\n");
            for (TransactionCategory category : candidateCategories) {
                sb.append(String.format("- ID: %d, Nazwa: %s\n", category.getId(), category.getName()));
            }
            sb.append("\nWybierz dokładnie jedną kategorię (categoryId) najlepiej pasującą do transakcji.");
        }

        return sb.toString();
    }

    private String buildRequestBody(String prompt, List<TransactionCategory> candidateCategories) throws Exception {
        return objectMapper.writeValueAsString(new AnthropicRequest(
                model,
                1024,
                new AnthropicMessage[]{
                        new AnthropicMessage("user", prompt)
                },
                new OutputConfig(new ResponseFormat("json_schema", buildResponseSchema(candidateCategories)))
        ));
    }

    private Map<String, Object> buildResponseSchema(List<TransactionCategory> candidateCategories) {
        Map<String, Object> firmIdSchema = Map.of("type", "integer");

        Map<String, Object> labelIdsSchema = new LinkedHashMap<>();
        labelIdsSchema.put("type", "array");
        labelIdsSchema.put("items", Map.of("type", "integer"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("firmId", firmIdSchema);
        properties.put("labelIds", labelIdsSchema);

        List<String> required = new ArrayList<>(List.of("firmId", "labelIds"));

        // Only ask for a category when there is anything to choose from - constraining via
        // enum keeps Claude from ever returning an id that doesn't exist.
        if (!candidateCategories.isEmpty()) {
            Map<String, Object> categoryIdSchema = new LinkedHashMap<>();
            categoryIdSchema.put("type", "integer");
            categoryIdSchema.put("enum", candidateCategories.stream().map(TransactionCategory::getId).toList());
            properties.put("categoryId", categoryIdSchema);
            required.add("categoryId");
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private MatchResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentArray = root.get("content");

            if (contentArray != null && contentArray.isArray() && !contentArray.isEmpty()) {
                // output_config.format guarantees the first block is text with schema-valid JSON
                String text = contentArray.get(0).get("text").asText();
                JsonNode jsonNode = objectMapper.readTree(text);

                Integer firmId = jsonNode.get("firmId").isNull() ? defaultFirmId : jsonNode.get("firmId").asInt();
                List<Integer> labelIds = new ArrayList<>();

                JsonNode labelIdsNode = jsonNode.get("labelIds");
                if (labelIdsNode != null && labelIdsNode.isArray()) {
                    for (JsonNode labelId : labelIdsNode) {
                        labelIds.add(labelId.asInt());
                    }
                }

                JsonNode categoryIdNode = jsonNode.get("categoryId");
                Integer categoryId = (categoryIdNode == null || categoryIdNode.isNull()) ? null : categoryIdNode.asInt();

                return new MatchResult(firmId, labelIds, categoryId);
            }
        } catch (Exception e) {
            log.debug("Error parsing Claude API response: {}", e.getMessage());
        }
        return new MatchResult(defaultFirmId, new ArrayList<>(), null);
    }

    public static class MatchResult {
        public final Integer firmId;
        public final List<Integer> labelIds;
        /**
         * Id kategorii wskazanej przez Claude, ograniczonej do listy candidateCategories przekazanej do match().
         * Null, gdy nie przekazano żadnych kandydatów albo dopasowanie się nie powiodło (np. brak klucza API,
         * błąd HTTP lub wyjątek) - w takim wypadku o wartości domyślnej decyduje wywołujący, bo tylko on zna
         * typ transakcji (EXPENSE/INCOME).
         */
        public final Integer categoryId;

        public MatchResult(Integer firmId, List<Integer> labelIds, Integer categoryId) {
            this.firmId = firmId;
            this.labelIds = labelIds;
            this.categoryId = categoryId;
        }
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
