package net.focik.homeoffice.finance.domain.transaction;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.domain.transaction.model.*;
import net.focik.homeoffice.finance.domain.transaction.port.primary.*;
import net.focik.homeoffice.finance.infrastructure.util.CsvParser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@AllArgsConstructor
public class BankTransactionFacade implements
        AddBankTransactionUseCase, UpdateBankTransactionUseCase, GetBankTransactionUseCase, DeleteBankTransactionUseCase,
        AddTransactionCategoryUseCase, UpdateTransactionCategoryUseCase, GetTransactionCategoryUseCase, DeleteTransactionCategoryUseCase,
        AddTransactionLabelUseCase, UpdateTransactionLabelUseCase, GetTransactionLabelUseCase, DeleteTransactionLabelUseCase,
        ImportBankTransactionsUseCase {

    private final BankTransactionService bankTransactionService;
    private final TransactionCategoryService transactionCategoryService;
    private final TransactionLabelService transactionLabelService;
    private final TransactionImportService transactionImportService;
    private final CsvParser csvParser;

    // BankTransaction operations
    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "BankTransaction")
    public BankTransaction addBankTransaction(BankTransaction bankTransaction) {
        return bankTransactionService.addBankTransaction(bankTransaction);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "BankTransaction")
    public BankTransaction updateBankTransaction(BankTransaction bankTransaction) {
        return bankTransactionService.updateBankTransaction(bankTransaction);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "BankTransaction")
    public void deleteBankTransaction(int id) {
        bankTransactionService.deleteBankTransaction(id);
    }

    @Override
    public BankTransaction findById(int id) {
        return bankTransactionService.findBankTransactionById(id);
    }

    @Override
    public List<BankTransaction> findBetween(LocalDate dateFrom, LocalDate dateTo, int idUser) {
        return bankTransactionService.findBankTransactionBetween(idUser, dateFrom, dateTo);
    }

    // TransactionCategory operations
    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "TransactionCategory")
    public TransactionCategory addTransactionCategory(TransactionCategory transactionCategory) {
        return transactionCategoryService.addTransactionCategory(transactionCategory);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "TransactionCategory")
    public TransactionCategory updateTransactionCategory(TransactionCategory transactionCategory) {
        return transactionCategoryService.updateTransactionCategory(transactionCategory);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "TransactionCategory")
    public void deleteTransactionCategory(int id) {
        transactionCategoryService.deleteTransactionCategory(id);
    }

    @Override
    public List<TransactionCategory> getAllTransactionCategories() {
        return transactionCategoryService.findAllTransactionCategories();
    }

    @Override
    public TransactionCategory getTransactionCategoryById(int id) {
        return transactionCategoryService.findTransactionCategoryById(id);
    }

    // TransactionLabel operations
    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "TransactionLabel")
    public TransactionLabel addTransactionLabel(TransactionLabel transactionLabel) {
        return transactionLabelService.addTransactionLabel(transactionLabel);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "TransactionLabel")
    public TransactionLabel updateTransactionLabel(TransactionLabel transactionLabel) {
        return transactionLabelService.updateTransactionLabel(transactionLabel);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "TransactionLabel")
    public void deleteTransactionLabel(int id) {
        transactionLabelService.deleteTransactionLabel(id);
    }

    @Override
    public List<TransactionLabel> getAllTransactionLabels() {
        return transactionLabelService.findAllTransactionLabels();
    }

    @Override
    public TransactionLabel getTransactionLabelById(int id) {
        return transactionLabelService.findTransactionLabelById(id);
    }

    // Import operations
    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "BankTransaction")
    public TransactionImportResult importFromCsv(byte[] fileContent, int idUser, boolean testMode) {

        List<CsvTransactionRow> csvRows;
        try {
            csvRows = csvParser.parseCsv(fileContent);
        } catch (Exception e) {
            return TransactionImportResult.builder()
                    .successCount(0)
                    .failedCount(1)
                    .totalProcessed(0)
                    .errors(List.of(TransactionImportError.builder()
                            .rowNumber(0)
                            .errorMessage("Błąd parsowania CSV: " + e.getMessage())
                            .build()))
                    .importDateTime(LocalDateTime.now())
                    .build();
        }
        TransactionImportResult transactionImportResult = transactionImportService.importFromCsv(csvRows, idUser);
        if (!testMode) {
            transactionImportResult.getImportedTransactions()
                    .forEach(bankTransactionService::addBankTransaction);
        }
        return transactionImportResult;
    }
}
