package net.focik.homeoffice.finance.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.audit.AuditableEntity;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "finance_transaction")
@Getter
@ToString
@Builder
public class BankTransactionDbDto extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer idFirm;
    private Integer idUser;
    private Integer purchaseId;
    private String description;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    private Integer transactionCategoryId;
    @ElementCollection
    @CollectionTable(name = "bank_transaction_labels", joinColumns = @JoinColumn(name = "transaction_id"))
    @Column(name = "label_id")
    private List<Integer> transactionLabelIds;
    private boolean boughtOnCredit;
}
