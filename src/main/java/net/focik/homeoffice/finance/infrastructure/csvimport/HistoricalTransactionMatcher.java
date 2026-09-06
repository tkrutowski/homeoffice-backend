package net.focik.homeoffice.finance.infrastructure.csvimport;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Zanim zapytamy Claude, sprawdzamy czy dla tego samego użytkownika istnieje już zapisana
 * transakcja o identycznym (znormalizowanym) opisie. Jeśli cała historia jednoznacznie wskazuje
 * jedną firmę, traktujemy to jako pewniejsze niż zgadywanie AI - to sam użytkownik tak to wcześniej
 * skorygował - i pomijamy wywołanie AI. Dopasowanie po firmie i po kategorii jest niezależne:
 * kategoria jest reużywana tylko, gdy historia też jest co do niej jednoznaczna.
 */
@Slf4j
@Component
class HistoricalTransactionMatcher {

    Optional<HistoricalMatch> findMatch(String description, List<BankTransaction> history) {
        String key = normalize(description);
        if (key.isEmpty()) {
            return Optional.empty();
        }

        List<BankTransaction> candidates = history.stream()
                .filter(t -> t.getIdFirm() > 0)
                .filter(t -> key.equals(normalize(t.getDescription())))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Set<Integer> distinctFirms = candidates.stream()
                .map(BankTransaction::getIdFirm)
                .collect(Collectors.toSet());
        if (distinctFirms.size() != 1) {
            log.debug("Historia niejednoznaczna dla '{}' ({} różnych firm) - pomijam dopasowanie historyczne",
                    description, distinctFirms.size());
            return Optional.empty();
        }
        int firmId = distinctFirms.iterator().next();

        Set<Integer> distinctCategories = candidates.stream()
                .map(BankTransaction::getTransactionCategory)
                .filter(Objects::nonNull)
                .map(TransactionCategory::getId)
                .collect(Collectors.toSet());
        Integer categoryId = distinctCategories.size() == 1 ? distinctCategories.iterator().next() : null;

        List<TransactionLabel> labels = candidates.stream()
                .max(Comparator.comparing(BankTransaction::getTransactionDate))
                .map(BankTransaction::getTransactionLabel)
                .orElseGet(ArrayList::new);

        log.info("Dopasowanie historyczne dla '{}': firma={}, kategoria={} (na podstawie {} wcześniejszych transakcji)",
                description, firmId, categoryId, candidates.size());

        return Optional.of(new HistoricalMatch(firmId, categoryId, labels));
    }

    /**
     * Wariant dla zakupów kartą (Purchase nie ma w domenie kategorii ani etykiet - dopasowujemy
     * tylko firmę). Gdy cała historia o identycznym (znormalizowanym) opisie wskazuje jedną firmę,
     * reużywamy ją. Gdy historia jest niejednoznaczna (różne firmy pod tym samym opisem), zakładamy,
     * że to użytkownik świadomie zmienił przypisanie i bierzemy firmę z najnowszego (po purchaseDate)
     * pasującego zakupu - podobnie jak przy etykietach w findMatch().
     */
    Optional<Integer> findFirmMatch(String description, List<Purchase> history) {
        String key = normalize(description);
        if (key.isEmpty()) {
            return Optional.empty();
        }

        List<Purchase> candidates = history.stream()
                .filter(p -> p.getIdFirm() > 0)
                .filter(p -> key.equals(normalize(p.getName())))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Set<Integer> distinctFirms = candidates.stream()
                .map(Purchase::getIdFirm)
                .collect(Collectors.toSet());

        int firmId;
        if (distinctFirms.size() == 1) {
            firmId = distinctFirms.iterator().next();
        } else {
            firmId = candidates.stream()
                    .max(Comparator.comparing(Purchase::getPurchaseDate))
                    .map(Purchase::getIdFirm)
                    .orElseThrow();
            log.debug("Historia zakupów niejednoznaczna dla '{}' ({} różnych firm) - wybieram najnowszą: firma={}",
                    description, distinctFirms.size(), firmId);
        }

        log.info("Dopasowanie historyczne (zakup) dla '{}': firma={}", description, firmId);
        return Optional.of(firmId);
    }

    /**
     * Usuwa cyfry (numery sklepów, daty, numery referencyjne) i interpunkcję, żeby ten sam
     * sprzedawca/odbiorca pojawiający się w różnych dniach/placówkach dał ten sam klucz.
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toUpperCase()
                .replaceAll("[0-9]+", "")
                .replaceAll("[^\\p{L}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    record HistoricalMatch(int firmId, Integer categoryId, List<TransactionLabel> labels) {
    }
}
