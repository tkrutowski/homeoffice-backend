package net.focik.homeoffice.finance.domain.transaction.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class TransactionImportResult {
    private int successCount;
    private int failedCount;
    private int totalProcessed;
    private List<BankTransaction> importedTransactions;
    private List<TransactionImportError> errors;
    private LocalDateTime importDateTime;

    public String getStatus() {
        if (failedCount == 0) {
            return "SUCCESS";
        }
        if (successCount == 0) {
            return "FAILED";
        }
        return "PARTIAL_SUCCESS";
    }
}
