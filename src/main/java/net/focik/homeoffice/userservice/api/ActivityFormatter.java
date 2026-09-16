package net.focik.homeoffice.userservice.api;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditEntry;
import net.focik.homeoffice.userservice.api.dto.UserActivityDto;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Turns a raw {@link AuditEntry} into a short, human-readable activity line
 * for the "Aktywność konta" tile, without ever exposing the entry's raw
 * new_values JSON payload to the client.
 */
@Component
@RequiredArgsConstructor
public class ActivityFormatter {

    private static final Map<String, String> MODULE_TAGS = Map.ofEntries(
            Map.entry("Fee", "Finanse"), Map.entry("FeeInstallment", "Finanse"),
            Map.entry("Loan", "Finanse"), Map.entry("LoanInstallment", "Finanse"),
            Map.entry("Purchase", "Finanse"), Map.entry("Card", "Finanse"),
            Map.entry("Bank", "Finanse"), Map.entry("BankTransaction", "Finanse"),
            Map.entry("TransactionCategory", "Finanse"), Map.entry("TransactionLabel", "Finanse"),
            Map.entry("Firm", "Finanse"),
            Map.entry("Book", "Biblioteka"), Map.entry("Bookstore", "Biblioteka"),
            Map.entry("UserBook", "Biblioteka"), Map.entry("Author", "Biblioteka"),
            Map.entry("Category", "Biblioteka"), Map.entry("Series", "Biblioteka"),
            Map.entry("Device", "Urządzenia"), Map.entry("Computer", "Urządzenia"),
            Map.entry("DeviceType", "Urządzenia"),
            Map.entry("User", "Konto"),
            Map.entry("Cost", "Faktury"), Map.entry("Customer", "Faktury"),
            Map.entry("Supplier", "Faktury"), Map.entry("Company", "Faktury"),
            Map.entry("Invoice", "Faktury"),
            Map.entry("Address", "Adresy")
    );

    private static final Map<String, String> NOUNS = Map.ofEntries(
            Map.entry("Fee", "opłatę"), Map.entry("FeeInstallment", "ratę opłaty"),
            Map.entry("Loan", "kredyt"), Map.entry("LoanInstallment", "ratę kredytu"),
            Map.entry("Purchase", "zakup"), Map.entry("Card", "kartę płatniczą"),
            Map.entry("Bank", "bank"), Map.entry("BankTransaction", "transakcję bankową"),
            Map.entry("TransactionCategory", "kategorię transakcji"),
            Map.entry("TransactionLabel", "etykietę transakcji"),
            Map.entry("Firm", "firmę"),
            Map.entry("Book", "książkę"), Map.entry("Bookstore", "księgarnię"),
            Map.entry("UserBook", "pozycję w biblioteczce"), Map.entry("Author", "autora"),
            Map.entry("Category", "kategorię"), Map.entry("Series", "serię"),
            Map.entry("Device", "urządzenie"), Map.entry("Computer", "komputer"),
            Map.entry("DeviceType", "typ urządzenia"),
            Map.entry("Cost", "koszt"), Map.entry("Customer", "klienta"),
            Map.entry("Supplier", "dostawcę"), Map.entry("Company", "firmę"),
            Map.entry("Invoice", "fakturę"),
            Map.entry("Address", "adres")
    );

    private static final Map<AuditAction, String> VERBS = Map.of(
            AuditAction.CREATE, "Dodano",
            AuditAction.UPDATE, "Zaktualizowano",
            AuditAction.DELETE, "Usunięto"
    );

    // Best-effort candidates, tried in order, for the entity's display name.
    // Not every entity type has one of these fields - when none match, the
    // sentence is rendered without a quoted name instead of failing.
    private static final List<String> LABEL_FIELDS =
            List.of("title", "name", "cardName", "description");

    private final ObjectMapper objectMapper;

    public UserActivityDto format(AuditEntry entry) {
        String moduleTag = MODULE_TAGS.getOrDefault(entry.getEntityType(), "Inne");
        String verb = VERBS.getOrDefault(entry.getAction(), "Zmieniono");

        String summary;
        if ("User".equals(entry.getEntityType())) {
            summary = "Zaktualizowano dane profilu";
        } else {
            String noun = NOUNS.getOrDefault(entry.getEntityType(), entry.getEntityType());
            String label = extractLabel(entry.getNewValues());
            summary = label != null ? verb + " " + noun + " „" + label + "”" : verb + " " + noun;
        }

        return new UserActivityDto(summary, moduleTag, entry.getAction().name(), entry.getChangedAt());
    }

    private String extractLabel(String newValuesJson) {
        if (newValuesJson == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(newValuesJson);
            for (String field : LABEL_FIELDS) {
                if (node.has(field)) {
                    String value = node.get(field).asString(null);
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            // Best-effort formatting only - a malformed/unexpected payload
            // just means the sentence renders without a quoted name.
        }
        return null;
    }
}
