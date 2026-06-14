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
import net.focik.homeoffice.finance.domain.purchase.port.secondary.PurchaseRepository;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionType;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.BankTransactionRepository;
import net.focik.homeoffice.finance.domain.card.port.primary.GetCardUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.GetFirmUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GetTransactionLabelUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankCsvAsyncWorker {

    private final MilleniumBankCsvParser csvParser;
    private final ClaudeAiMatcher aiMatcher;
    private final GetFirmUseCase getFirmUseCase;
    private final GetCardUseCase getCardUseCase;
    private final GetTransactionLabelUseCase getTransactionLabelUseCase;
    private final BankTransactionRepository bankTransactionRepository;
    private final PurchaseRepository purchaseRepository;
    private final AsyncTaskService asyncTaskService;
    private final ObjectMapper objectMapper;

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

                    int transactionCount = 0;
                    int purchaseCount = 0;
                    int duplicateCount = 0;

                    for (RawBankCsvRecord record : parseResult.records) {
                        try {
                            if (record.isAccountRow()) {
                                BankTransactionImportDto dto = processBankTransaction(record, idUser, allFirms, allLabels);
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
                                    PurchaseImportDto dto = processPurchase(record, idUser, allFirms);
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

    private BankTransactionImportDto processBankTransaction(RawBankCsvRecord record, int idUser, List<Firm> allFirms, List<TransactionLabel> allLabels) {
        String matchText = (record.getRecipientSender() + " " + record.getDescription()).trim();
        ClaudeAiMatcher.MatchResult matchResult = aiMatcher.match(matchText, allFirms, allLabels);

        int firmId = matchResult.firmId != null ? matchResult.firmId : 0;
        List<TransactionLabel> labels = new ArrayList<>();
        for (Integer labelId : matchResult.labelIds) {
            TransactionLabel label = getTransactionLabelUseCase.getTransactionLabelById(labelId);
            if (label != null) {
                labels.add(label);
            }
        }

        BigDecimal amount = record.getDebit() != null ? record.getDebit().abs() : record.getCredit().abs();
        TransactionType type = record.getDebit() != null ? TransactionType.TRANSFER_OUT : TransactionType.TRANSFER_IN;

        boolean exists = bankTransactionRepository.existsByTransactionDateAndAmountAndIdUser(
                record.getTransactionDate(), amount, idUser
        );

        return BankTransactionImportDto.builder()
                .idFirm(firmId)
                .idUser(idUser)
                .description(record.getDescription())
                .transactionDate(record.getTransactionDate())
                .amount(amount.toString())
                .transactionType(type)
                .transactionLabel(labels)
                .exists(exists)
                .balance(record.getBalance() != null ? record.getBalance().toString() : null)
                .build();
    }

    private PurchaseImportDto processPurchase(RawBankCsvRecord record, int idUser, List<Firm> allFirms) {
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
        ClaudeAiMatcher.MatchResult matchResult = aiMatcher.match(matchText, allFirms, new ArrayList<>());

        int firmId = matchResult.firmId != null ? matchResult.firmId : 0;
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
