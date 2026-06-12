package net.focik.homeoffice.finance.api.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class BankTransactionImportResponse {
    private int successCount;
    private int failedCount;
    private int totalProcessed;
    private String status;
    private List<BankTransactionDto> importedTransactions;
    private List<ImportErrorDto> errors;
    private LocalDateTime importDateTime;
}
