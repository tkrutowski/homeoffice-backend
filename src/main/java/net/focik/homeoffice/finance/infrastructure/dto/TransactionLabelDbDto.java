package net.focik.homeoffice.finance.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.audit.AuditableEntity;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "finance_transaction_label")
@Getter
@Setter
@ToString
@Builder
public class TransactionLabelDbDto extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
}
