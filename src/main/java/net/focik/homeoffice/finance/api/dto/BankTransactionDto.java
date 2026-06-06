package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionType;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class BankTransactionDto {
    private int id;
    private int idFirm;
    private int idUser;
    private List<Integer> purchaseIds;
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate transactionDate;
    private String amount;
    private TransactionType transactionType;
    private TransactionCategory transactionCategory;
    private List<TransactionLabel> transactionLabel;
    private boolean boughtOnCredit;
}
