package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.audit.AuditableEntity;
import net.focik.homeoffice.finance.domain.card.CardType;
import net.focik.homeoffice.utils.share.ActiveStatus;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "finance_card")
@Data
@ToString
@Builder
public class CardDbDto extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer idBank;
    private Integer idUser;
    private String cardName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate activationDate;
    @Column(name = "card_limit")
    private Integer limit;
    @Enumerated(EnumType.STRING)
    private CardType cardType;
    private Integer closingDay;
    private Integer repaymentDay;
    private Integer paymentTermDays;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private ActiveStatus activeStatus;
    private String cardNumber;
    private String imageUrl;
    private Boolean multi;
}