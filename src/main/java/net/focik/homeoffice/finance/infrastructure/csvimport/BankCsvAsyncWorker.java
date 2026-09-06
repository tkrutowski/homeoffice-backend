package net.focik.homeoffice.finance.infrastructure.csvimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.audit.AsyncContext;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.finance.api.dto.BankCsvImportResponse;
import net.focik.homeoffice.finance.api.dto.BankTransactionImportDto;
import net.focik.homeoffice.finance.api.dto.PurchaseImportDto;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.csvimport.RawBankCsvRecord;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.secondary.PurchaseRepository;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategoryType;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionType;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.BankTransactionRepository;
import net.focik.homeoffice.finance.domain.card.port.primary.GetCardUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.GetFirmUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GetTransactionCategoryUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GetTransactionLabelUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankCsvAsyncWorker {

    private final MilleniumBankCsvParser csvParser;
    private final ClaudeAiMatcher aiMatcher;
    private final HistoricalTransactionMatcher historicalTransactionMatcher;
    private final GetFirmUseCase getFirmUseCase;
    private final GetCardUseCase getCardUseCase;
    private final GetTransactionLabelUseCase getTransactionLabelUseCase;
    private final GetTransactionCategoryUseCase getTransactionCategoryUseCase;
    private final BankTransactionRepository bankTransactionRepository;
    private final PurchaseRepository purchaseRepository;
    private final AsyncTaskService asyncTaskService;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_EXPENSE_CATEGORY_ID = 24; // Inne

    @Async
    public void processJobAsync(String jobId, byte[] fileContent, int idUser) {
        AsyncContext.setJobType("MILLENIUM_BANK_CSV_IMPORT");
        try {
            AsyncTask job = asyncTaskService.getJobStatus(jobId);
            if (job == null) {
                log.warn("Job {} not found", jobId);
                return;
            }

            job.setStatus(AsyncTaskStatus.RUNNING);
            asyncTaskService.updateTask(job);

            log.info("Starting CSV import job: {} for user: {}", jobId, idUser);

            try {
                List<BankTransactionImportDto> transactions = new ArrayList<>();
                List<PurchaseImportDto> purchases = new ArrayList<>();

                MilleniumBankCsvParser.CsvParseResult parseResult = csvParser.parseCsv(fileContent);
                List<String> errors = new ArrayList<>(parseResult.errors);

                if (!parseResult.records.isEmpty()) {
                    List<Firm> allFirms = getFirmUseCase.findByAll();
                    List<TransactionLabel> allLabels = getTransactionLabelUseCase.getAllTransactionLabels();
                    List<TransactionCategory> allCategories = getTransactionCategoryUseCase.getAllTransactionCategories();
                    List<BankTransaction> history = bankTransactionRepository.findBankTransactionByUserId(idUser);
                    List<Purchase> purchaseHistory = purchaseRepository.findPurchaseByUserId(idUser);

                    int transactionCount = 0;
                    int purchaseCount = 0;
                    int duplicateCount = 0;

                    for (RawBankCsvRecord record : parseResult.records) {
                        try {
                            if (record.isAccountRow()) {
                                BankTransactionImportDto dto = processBankTransaction(record, idUser, allFirms, allLabels, allCategories, history);
                                if (dto != null) {
                                    transactions.add(dto);
                                    if (dto.isExists()) {
                                        duplicateCount++;
                                    }
                                    transactionCount++;
                                }
                            } else {
                                // Only process if there's a debit (expense)
                                if (record.getDebit() != null) {
                                    PurchaseImportDto dto = processPurchase(record, idUser, allFirms, purchaseHistory);
                                    if (dto != null) {
                                        purchases.add(dto);
                                        if (dto.isExists()) {
                                            duplicateCount++;
                                        }
                                        purchaseCount++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            errors.add("Wiersz " + record.getRowNumber() + ": " + e.getMessage());
                            log.debug("Error processing row {}: {}", record.getRowNumber(), e.getMessage());
                        }
                    }

                    BankCsvImportResponse result = BankCsvImportResponse.builder()
                            .totalProcessed(transactionCount + purchaseCount)
                            .transactionCount(transactionCount)
                            .purchaseCount(purchaseCount)
                            .duplicateCount(duplicateCount)
                            .transactions(transactions)
                            .purchases(purchases)
                            .errors(errors)
                            .build();

                    String resultJson = objectMapper.writeValueAsString(result);
                    job.setTextractResultJson(resultJson);
                    job.setProcessed(transactionCount + purchaseCount);

                    job.setStatus(AsyncTaskStatus.SUCCEEDED);
                    job.setMessage("Pomyślnie przetworzono " + (transactionCount + purchaseCount) + " wierszy.");
                } else {
                    job.setStatus(AsyncTaskStatus.FAILED);
                    job.setMessage("Brak wierszy do przetworzenia.");
                }

            } catch (Exception e) {
                log.error("Error processing CSV import job {}", jobId, e);
                job.setStatus(AsyncTaskStatus.FAILED);
                job.setMessage("Błąd przetwarzania: " + e.getMessage());
                job.getErrors().add(new AsyncTaskError(null, e.getMessage()));
            }

            asyncTaskService.updateTask(job);
            log.info("Finished CSV import job: {} with status: {}", jobId, job.getStatus());
        } finally {
            AsyncContext.clear();
        }
    }

    private BankTransactionImportDto processBankTransaction(RawBankCsvRecord record, int idUser, List<Firm> allFirms,
                                                              List<TransactionLabel> allLabels, List<TransactionCategory> allCategories,
                                                              List<BankTransaction> history) {
        TransactionType type = record.getDebit() != null ? TransactionType.TRANSFER_OUT : TransactionType.TRANSFER_IN;
        TransactionCategoryType expectedCategoryType = type == TransactionType.TRANSFER_OUT
                ? TransactionCategoryType.EXPENSE : TransactionCategoryType.INCOME;

        // Scalamy odbiorcę/zleceniodawcę z opisem - dla przelewów sam "Opis" bywa identyczny dla
        // różnych osób (np. "Przelew BLIK na telefon"), a to właśnie odbiorca niesie tożsamość.
        // Ten sam, wzbogacony tekst trafia do usera (pole description) i do bazy, dzięki czemu
        // przyszłe importy mogą się do niego odwołać przez HistoricalTransactionMatcher.
        String description = joinNonBlank(record.getRecipientSender(), record.getDescription());

        Optional<HistoricalTransactionMatcher.HistoricalMatch> historicalMatch =
                historicalTransactionMatcher.findMatch(description, history);

        int firmId;
        List<TransactionLabel> labels;
        Integer categoryId;

        if (historicalMatch.isPresent()) {
            HistoricalTransactionMatcher.HistoricalMatch match = historicalMatch.get();
            firmId = match.firmId();
            labels = match.labels();
            categoryId = match.categoryId();
        } else {
            List<TransactionCategory> candidateCategories = allCategories.stream()
                    .filter(c -> c.getType() == expectedCategoryType)
                    .toList();

            ClaudeAiMatcher.MatchResult matchResult = aiMatcher.match(description, allFirms, allLabels, candidateCategories);

            firmId = matchResult.firmId != null ? matchResult.firmId : 0;
            labels = new ArrayList<>();
            for (Integer labelId : matchResult.labelIds) {
                TransactionLabel label = getTransactionLabelUseCase.getTransactionLabelById(labelId);
                if (label != null) {
                    labels.add(label);
                }
            }
            categoryId = matchResult.categoryId;
        }

        // Brak dopasowania (ani historycznego, ani od AI): dla wydatku wracamy do kategorii "Inne",
        // dla przychodu zostawiamy null - użytkownik uzupełnia ją ręcznie w podglądzie importu.
        if (categoryId == null && expectedCategoryType == TransactionCategoryType.EXPENSE) {
            categoryId = DEFAULT_EXPENSE_CATEGORY_ID;
        }
        TransactionCategory category = categoryId != null
                ? getTransactionCategoryUseCase.getTransactionCategoryById(categoryId)
                : null;

        BigDecimal amount = record.getDebit() != null ? record.getDebit().abs() : record.getCredit().abs();

        boolean exists = bankTransactionRepository.existsByTransactionDateAndAmountAndIdUser(
                record.getTransactionDate(), amount, idUser
        );

        return BankTransactionImportDto.builder()
                .idFirm(firmId)
                .idUser(idUser)
                .description(description)
                .transactionDate(record.getTransactionDate())
                .amount(amount.toString())
                .transactionType(type)
                .transactionLabel(labels)
                .transactionCategory(category)
                .exists(exists)
                .balance(record.getBalance() != null ? record.getBalance().toString() : null)
                .build();
    }

    private static String joinNonBlank(String... parts) {
        return Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private PurchaseImportDto processPurchase(RawBankCsvRecord record, int idUser, List<Firm> allFirms,
                                               List<Purchase> purchaseHistory) {
        String last4Digits = record.getLastFourDigits();
        Card matchedCard = null;

        if (!last4Digits.isEmpty()) {
            List<Card> allCards = getCardUseCase.findAll();
            for (Card card : allCards) {
                if (card.getCardNumber() != null && card.getCardNumber().endsWith(last4Digits)) {
                    matchedCard = card;
                    break;
                }
            }
        }

        if (matchedCard == null) {
            log.warn("No card found for last4={}, skipping purchase", last4Digits);
            return null;
        }

        String matchText = (record.getDescription()).trim();
        // Purchase (zakup kartą) nie ma w domenie pola kategorii - kategorię dostaje dopiero
        // powiązana BankTransaction, tworzona automatycznie przy opłaceniu (PurchaseFacade.findCategoryByCard).
        Optional<Integer> historicalFirmId = historicalTransactionMatcher.findFirmMatch(matchText, purchaseHistory);

        int firmId;
        if (historicalFirmId.isPresent()) {
            firmId = historicalFirmId.get();
        } else {
            ClaudeAiMatcher.MatchResult matchResult = aiMatcher.match(matchText, allFirms, new ArrayList<>(), new ArrayList<>());
            firmId = matchResult.firmId != null ? matchResult.firmId : 0;
        }
        boolean exists = purchaseRepository.existsByPurchaseDateAndAmountAndIdUser(
                record.getTransactionDate(), record.getDebit().abs(), idUser
        );

        return PurchaseImportDto.builder()
                .idCard(matchedCard.getId())
                .idFirm(firmId)
                .idUser(idUser)
                .name(record.getDescription())
                .purchaseDate(record.getTransactionDate())
                .amount(record.getDebit().abs().toString())
                .otherInfo(record.getDescription())
                .exists(exists)
                .build();
    }
}
