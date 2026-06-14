package net.focik.homeoffice.finance.api.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class BankCsvImportResponse {
    private int totalProcessed;
    private int transactionCount;
    private int purchaseCount;
    private int duplicateCount;
    private List<BankTransactionImportDto> transactions;
    private List<PurchaseImportDto> purchases;
    private List<String> errors;
}
