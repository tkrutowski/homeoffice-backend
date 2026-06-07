package net.focik.homeoffice.finance.domain.transaction.model;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class BankTransaction {

    private Integer id;
    private int idFirm;
    private int idUser;
    private Integer purchaseId;
    private String description;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionCategory transactionCategory;
    private List<TransactionLabel> transactionLabel;
    private boolean boughtOnCredit;

}
