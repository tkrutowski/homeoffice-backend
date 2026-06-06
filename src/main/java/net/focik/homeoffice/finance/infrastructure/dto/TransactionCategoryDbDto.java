package net.focik.homeoffice.finance.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.audit.AuditableEntity;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategoryType;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "finance_transaction_category")
@Getter
@Setter
@ToString
@Builder
public class TransactionCategoryDbDto extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    @Enumerated(EnumType.STRING)
    private TransactionCategoryType type;
    private String color;
    private String icon;
}
