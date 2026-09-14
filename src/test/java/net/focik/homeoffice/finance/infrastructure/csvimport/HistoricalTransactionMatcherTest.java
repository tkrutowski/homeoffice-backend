package net.focik.homeoffice.finance.infrastructure.csvimport;

import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalTransactionMatcherTest {

    private final HistoricalTransactionMatcher matcher = new HistoricalTransactionMatcher();

    @Test
    @DisplayName("should return empty when the transaction history is empty")
    void findMatch_ShouldReturnEmpty_WhenHistoryIsEmpty() {
        Optional<HistoricalTransactionMatcher.HistoricalMatch> result =
                matcher.findMatch("ZABKA Z3762 K.2 POZNAN POL 2026-08-18", List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty when the description to match is blank")
    void findMatch_ShouldReturnEmpty_WhenDescriptionIsBlank() {
        List<BankTransaction> history = List.of(transaction("ZABKA", 5, null, null, LocalDate.now()));

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result = matcher.findMatch("   ", history);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty when no historical description matches")
    void findMatch_ShouldReturnEmpty_WhenNoHistoricalDescriptionMatches() {
        List<BankTransaction> history = List.of(transaction("LIDL 1 KRAKOW", 5, null, null, LocalDate.now()));

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result =
                matcher.findMatch("ZABKA Z3762 K.2 POZNAN POL 2026-08-18", history);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should ignore digits and punctuation when comparing descriptions, so store number/date variants still match")
    void findMatch_ShouldIgnoreDigitsAndPunctuation_WhenComparingDescriptions() {
        // Różne numery placówek i daty w tym samym sklepie powinny się znormalizować do tego samego klucza.
        List<BankTransaction> history = List.of(
                transaction("ZABKA Z3762 K.1  POZNAN POL 2026-08-17", 5, null, null, LocalDate.of(2026, 8, 17))
        );

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result =
                matcher.findMatch("ZABKA Z3762 K.2  POZNAN POL 2026-08-18", history);

        assertThat(result).isPresent();
        assertThat(result.get().firmId()).isEqualTo(5);
    }

    @Test
    @DisplayName("should ignore historical transactions that have no assigned firm")
    void findMatch_ShouldIgnoreTransactionsWithoutAssignedFirm() {
        List<BankTransaction> history = List.of(
                transaction("Stasiu Przelew BLIK na telefon", 0, null, null, LocalDate.now())
        );

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result =
                matcher.findMatch("Stasiu Przelew BLIK na telefon", history);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return the firm and category when all matching history entries agree on both")
    void findMatch_ShouldReturnUnanimousFirmAndCategory_WhenAllHistoryAgrees() {
        TransactionCategory groceries = category(2, "Spożywcze");
        List<BankTransaction> history = List.of(
                transaction("Stasiu Przelew BLIK na telefon", 6, groceries, null, LocalDate.of(2026, 8, 1)),
                transaction("Stasiu Przelew BLIK na telefon", 6, groceries, null, LocalDate.of(2026, 8, 10))
        );

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result =
                matcher.findMatch("Stasiu Przelew BLIK na telefon", history);

        assertThat(result).isPresent();
        assertThat(result.get().firmId()).isEqualTo(6);
        assertThat(result.get().categoryId()).isEqualTo(2);
    }

    @Test
    @DisplayName("should return empty when the matching history disagrees on the firm")
    void findMatch_ShouldReturnEmpty_WhenHistoryDisagreesOnFirm() {
        List<BankTransaction> history = List.of(
                transaction("Stasiu Przelew BLIK na telefon", 6, null, null, LocalDate.of(2026, 8, 1)),
                transaction("Stasiu Przelew BLIK na telefon", 7, null, null, LocalDate.of(2026, 8, 10))
        );

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result =
                matcher.findMatch("Stasiu Przelew BLIK na telefon", history);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return the firm with a null category when the history agrees on the firm but not the category")
    void findMatch_ShouldReturnNullCategory_WhenHistoryAgreesOnFirmButNotOnCategory() {
        List<BankTransaction> history = List.of(
                transaction("Stasiu Przelew BLIK na telefon", 6, category(2, "Spożywcze"), null, LocalDate.of(2026, 8, 1)),
                transaction("Stasiu Przelew BLIK na telefon", 6, category(3, "Rachunki"), null, LocalDate.of(2026, 8, 10))
        );

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result =
                matcher.findMatch("Stasiu Przelew BLIK na telefon", history);

        assertThat(result).isPresent();
        assertThat(result.get().firmId()).isEqualTo(6);
        assertThat(result.get().categoryId()).isNull();
    }

    @Test
    @DisplayName("should return the labels from the most recent matching historical transaction")
    void findMatch_ShouldReturnLabelsFromMostRecentMatchingTransaction() {
        TransactionLabel oldLabel = label(1, "old");
        TransactionLabel newLabel = label(2, "new");
        List<BankTransaction> history = List.of(
                transaction("Netflix.com", 9, null, List.of(oldLabel), LocalDate.of(2026, 7, 1)),
                transaction("Netflix.com", 9, null, List.of(newLabel), LocalDate.of(2026, 8, 1))
        );

        Optional<HistoricalTransactionMatcher.HistoricalMatch> result = matcher.findMatch("Netflix.com", history);

        assertThat(result).isPresent();
        assertThat(result.get().labels()).containsExactly(newLabel);
    }

    @Test
    @DisplayName("should return empty when the purchase history is empty")
    void findFirmMatch_ShouldReturnEmpty_WhenPurchaseHistoryIsEmpty() {
        Optional<Integer> result = matcher.findFirmMatch("ZABKA Z3762 K.2 POZNAN POL", List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty when no purchase in the history matches the name")
    void findFirmMatch_ShouldReturnEmpty_WhenNoPurchaseMatches() {
        List<Purchase> history = List.of(purchase("LIDL 1 KRAKOW", 5, LocalDate.now()));

        Optional<Integer> result = matcher.findFirmMatch("ZABKA Z3762 K.2 POZNAN POL", history);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return the firm id when all matching past purchases agree on it")
    void findFirmMatch_ShouldReturnFirm_WhenAllMatchingPurchasesAgree() {
        List<Purchase> history = List.of(
                purchase("Netflix.com", 9, LocalDate.of(2026, 7, 1)),
                purchase("Netflix.com", 9, LocalDate.of(2026, 8, 1))
        );

        Optional<Integer> result = matcher.findFirmMatch("Netflix.com", history);

        assertThat(result).contains(9);
    }

    @Test
    @DisplayName("should ignore digits and punctuation when comparing purchase names")
    void findFirmMatch_ShouldIgnoreDigitsAndPunctuation_WhenComparingPurchaseNames() {
        List<Purchase> history = List.of(purchase("ZABKA Z3762 K.1  POZNAN POL", 5, LocalDate.now()));

        Optional<Integer> result = matcher.findFirmMatch("ZABKA Z3762 K.2  POZNAN POL", history);

        assertThat(result).contains(5);
    }

    @Test
    @DisplayName("should return the most recent purchase's firm id when the purchase history disagrees on the firm")
    void findFirmMatch_ShouldReturnMostRecentFirm_WhenPurchaseHistoryDisagreesOnFirm() {
        List<Purchase> history = List.of(
                purchase("Netflix.com", 9, LocalDate.of(2026, 8, 1)),
                purchase("Netflix.com", 12, LocalDate.of(2026, 8, 10))
        );

        Optional<Integer> result = matcher.findFirmMatch("Netflix.com", history);

        assertThat(result).contains(12);
    }

    @Test
    @DisplayName("should ignore past purchases that have no assigned firm")
    void findFirmMatch_ShouldIgnorePurchasesWithoutAssignedFirm() {
        List<Purchase> history = List.of(purchase("Netflix.com", 0, LocalDate.now()));

        Optional<Integer> result = matcher.findFirmMatch("Netflix.com", history);

        assertThat(result).isEmpty();
    }

    private static Purchase purchase(String name, int idFirm, LocalDate purchaseDate) {
        return Purchase.builder().name(name).idFirm(idFirm).purchaseDate(purchaseDate).build();
    }

    private static BankTransaction transaction(String description, int idFirm, TransactionCategory category,
                                                List<TransactionLabel> labels, LocalDate date) {
        return BankTransaction.builder()
                .description(description)
                .idFirm(idFirm)
                .transactionCategory(category)
                .transactionLabel(labels)
                .transactionDate(date)
                .build();
    }

    private static TransactionCategory category(int id, String name) {
        return TransactionCategory.builder().id(id).name(name).build();
    }

    private static TransactionLabel label(int id, String name) {
        return TransactionLabel.builder().id(id).name(name).build();
    }
}
