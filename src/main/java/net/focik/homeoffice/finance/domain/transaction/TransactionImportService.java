package net.focik.homeoffice.finance.domain.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.domain.firm.FirmFacade;
import net.focik.homeoffice.finance.domain.transaction.model.*;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionImportService {

    private final TransactionCategoryService transactionCategoryService;
    private final TransactionLabelService transactionLabelService;
    private final FirmFacade firmFacade;

    private static final int DEFAULT_FIRM_ID = 38;//Inne
    private static final int DEFAULT_CATEGORY_ID = 24;//Inne
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };
    private static final Map<String, Integer> INCOME_CATEGORY_ID_MAP = Map.ofEntries(
            Map.entry("Biznes programowanie", 15),
            Map.entry("Extra dochód", 11),
            Map.entry("Gifts", 4),
            Map.entry("Jarek", 6),
            Map.entry("Pierożek", 14),
            Map.entry("Salary", 1)
    );
    private static final Map<String, Integer> EXPENSE_CATEGORY_ID_MAP = Map.ofEntries(
            Map.entry("Alfa spłata", 8),
            Map.entry("Car", 16),
            Map.entry("Entertainment", 18),
            Map.entry("Family & Personal", 10),
            Map.entry("focik.net", 5),
            Map.entry("Food & Drink", 19),
            Map.entry("Gifts", 20),
            Map.entry("Groceries", 2),
            Map.entry("Healthcare", 21),
            Map.entry("Home", 22),
            Map.entry("Pies", 25),
            Map.entry("IMPRESJA - spłata", 9),
            Map.entry("Kredyty", 7),
            Map.entry("Oszczędności", 23),
            Map.entry("Other", 24),
            Map.entry("Rachunki", 3),
            Map.entry("Shopping", 26),
            Map.entry("Sport & Hobbies", 18),
            Map.entry("Transport", 27),
            Map.entry("Wakacje", 28),
            Map.entry("Work", 29)
    );
    private static final Map<String, Integer> LABEL_ID_MAP = Map.<String, Integer>ofEntries(
            Map.entry("aga", 9),
            Map.entry("allegro", 10),
            Map.entry("apple tv", 11),
            Map.entry("apteka", 53),
            Map.entry("aws", 3),
            Map.entry("biedronka", 13),
            Map.entry("BMW", 2),
            Map.entry("BookBeat", 14),
            Map.entry("Castorama", 15),
            Map.entry("Cursor.ai", 16),
            Map.entry("delegacja", 17),
            Map.entry("dtp-soft", 54),
            Map.entry("empik", 18),
            Map.entry("Fryer", 19),
            Map.entry("Galaxy Buds", 20),
            Map.entry("GeForce5070Ti", 21),
            Map.entry("hbo", 22),
            Map.entry("ike", 55),
            Map.entry("ikea", 57),
            Map.entry("ikze", 56),
            Map.entry("inna-piekarnia", 58),
            Map.entry("Internet", 23),
            Map.entry("intimissimi", 24),
            Map.entry("kampery_kredyt", 25),
            Map.entry("komp2023", 26),
            Map.entry("kuba", 6),
            Map.entry("lidl", 27),
            Map.entry("Marche2024", 28),
            Map.entry("MediaMarkt", 59),
            Map.entry("Microsoft", 29),
            Map.entry("Millenium", 30),
            Map.entry("multikino", 31),
            Map.entry("MyFund", 32),
            Map.entry("n8n", 33),
            Map.entry("netflix", 34),
            Map.entry("Okulary", 35),
            Map.entry("orlen", 36),
            Map.entry("payPo", 37),
            Map.entry("peka", 38),
            Map.entry("play", 39),
            Map.entry("player.pl", 7),
            Map.entry("plaza", 41),
            Map.entry("pod strzechą", 40),
            Map.entry("prąd", 42),
            Map.entry("prime", 43),
            Map.entry("rav4", 1),
            Map.entry("S24", 60),
            Map.entry("skyshowtime", 44),
            Map.entry("spar", 45),
            Map.entry("Spotify", 46),
            Map.entry("star citizem", 47),
            Map.entry("stasiu", 5),
            Map.entry("Storytel", 48),
            Map.entry("tomek", 61),
            Map.entry("waga", 49),
            Map.entry("watch7", 50),
            Map.entry("webio", 51),
            Map.entry("żabka", 52)
    );

    public TransactionImportResult importFromCsv(List<CsvTransactionRow> rows, int idUser) {
        List<TransactionImportError> errors = new ArrayList<>();
        List<BankTransaction> validTransactions = new ArrayList<>();

        Map<String, Integer> firmsCache = buildFirmsCache();

        for (CsvTransactionRow row : rows) {
            try {
                BankTransaction transaction = mapRowToTransaction(row, idUser, firmsCache);
                validTransactions.add(transaction);
            } catch (Exception e) {
                errors.add(TransactionImportError.builder()
                        .rowNumber(row.getRowNumber())
                        .errorMessage(e.getMessage())
                        .csvLineContent(row.toString())
                        .build());
            }
        }

        return TransactionImportResult.builder()
                .successCount(validTransactions.size())
                .failedCount(errors.size())
                .totalProcessed(rows.size())
                .errors(errors)
                .importedTransactions(validTransactions)
                .importDateTime(LocalDateTime.now())
                .build();
    }

    private Map<String, Integer> buildFirmsCache() {
        Map<String, Integer> cache = new HashMap<>();
        List<Firm> allFirms = firmFacade.findByAll();
        for (Firm firm : allFirms) {
            cache.put(firm.getName().toLowerCase(), firm.getId());
        }
        return cache;
    }

    public BankTransaction mapRowToTransaction(CsvTransactionRow row, int idUser, Map<String, Integer> firmsCache) {
        if (row.getDate() == null) {
            throw new IllegalArgumentException("Data jest wymagana");
        }
        if (row.getAmount() == null || row.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Kwota nie może być pusta lub zerem");
        }
        if (row.getCategoryName() == null || row.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Kategoria jest wymagana");
        }
        if (row.getType() == null || row.getType().isBlank()) {
            throw new IllegalArgumentException("Typ jest wymagany");
        }

        TransactionCategory category = getCategory(row);

        List<TransactionLabel> labels = getLabels(row);

        return BankTransaction.builder()
                .idFirm(getFirmId(row, firmsCache))
                .idUser(idUser)
                .transactionDate(row.getDate())
                .amount(row.getAmount().abs())
                .transactionCategory(category)
                .description(row.getNote())
                .transactionType(getTransactionType(row))
                .transactionLabel(labels)
                .boughtOnCredit(false)
                .build();
    }

    @NonNull
    private static TransactionType getTransactionType(CsvTransactionRow row) {
        if (row.getType().equalsIgnoreCase("INCOME")) {
            return TransactionType.TRANSFER_IN;
        } else if (row.getType().equalsIgnoreCase("EXPENSE")) {
            return TransactionType.TRANSFER_OUT;
        } else {
            throw new IllegalArgumentException("Nieznany typ transakcji: " + row.getType());
        }
    }

    private List<TransactionLabel> getLabels(CsvTransactionRow row) {
        List<TransactionLabel> labels = new ArrayList<>();
        if (row.getLabels() != null && !row.getLabels().isBlank()) {
            String[] labelNames = row.getLabels().split(",");
            for (String labelName : labelNames) {
                String trimmedName = labelName.trim();
                Optional<TransactionLabel> label = Optional.ofNullable(LABEL_ID_MAP.get(trimmedName))
                        .map(transactionLabelService::findTransactionLabelById);
                label.ifPresent(labels::add);
            }
        }
        return labels;
    }

    @NonNull
    private TransactionCategory getCategory(CsvTransactionRow row) {
        TransactionCategory category;
        TransactionCategoryType type = TransactionCategoryType.valueOf(row.getType().toUpperCase());
        if (type == TransactionCategoryType.INCOME) {
        category = Optional.ofNullable(INCOME_CATEGORY_ID_MAP.get(row.getCategoryName()))
                .map(transactionCategoryService::findTransactionCategoryById)
                .orElse(null);
        }else {
            category = Optional.ofNullable(EXPENSE_CATEGORY_ID_MAP.get(row.getCategoryName()))
                    .map(transactionCategoryService::findTransactionCategoryById)
                    .orElse(null);
        }
        if (category == null) {
            category = transactionCategoryService.findCategoryByNameAndType(row.getCategoryName(), type)
                    .orElse(null);
        }
        if (category == null) {
            category = Optional.ofNullable(transactionCategoryService.findTransactionCategoryById(DEFAULT_CATEGORY_ID))
                    .orElseThrow(() -> new IllegalArgumentException("Kategoria '" + row.getCategoryName() + "' nie znaleziona"));
        }

        return category;
    }

    private int getFirmId(CsvTransactionRow row, Map<String, Integer> firmsCache) {
        String categoryLower = row.getCategoryName().toLowerCase();
        Integer firmId = firmsCache.get(categoryLower);

        if (firmId != null) {
            return firmId;
        }

        try {
            firmFacade.findByNameContaining(row.getCategoryName())
                    .map(Firm::getId)
                    .orElseGet(() -> {
                        log.warn("Nie znaleziono firmy dla kategorii '{}', ustawiam idFirm na {} -> 'Inne'", row.getCategoryName(), DEFAULT_FIRM_ID);
                        return DEFAULT_FIRM_ID;
                    });
        } catch (IncorrectResultSizeDataAccessException e) {
            log.warn("Znaleziono wiele firm dla kategorii '{}', ustawiam idFirm na {} -> 'Inne'", row.getCategoryName(), DEFAULT_FIRM_ID);
        }
        return DEFAULT_FIRM_ID;
    }

    public LocalDate parseDate(String dateString) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString, formatter);
            } catch (Exception e) {
                // Try next formatter
            }
        }
        throw new IllegalArgumentException("Nie można sparsować daty: " + dateString);
    }

    public BigDecimal parseAmount(String amountString) {
        if (amountString == null || amountString.isBlank()) {
            throw new IllegalArgumentException("Kwota nie może być pusta");
        }
        try {
            String normalized = amountString.replace(",", ".");
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Niepoprawna kwota: " + amountString);
        }
    }
}
