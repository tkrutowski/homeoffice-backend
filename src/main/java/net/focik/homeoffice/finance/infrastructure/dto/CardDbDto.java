package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.ActiveStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Finance_Card")
@Getter
@ToString
@Builder
public class CardDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer idBank;
    private Integer idUser;
    private String cardName;
    private LocalDate activationDate;
    private BigDecimal limit;
    private Integer repaymentDay;
    private LocalDate expirationDate;
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private ActiveStatus activeStatus;
    private String cardNumber;
    private Integer closingDay;
}