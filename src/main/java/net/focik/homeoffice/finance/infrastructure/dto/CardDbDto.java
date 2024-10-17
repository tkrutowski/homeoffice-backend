package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.ActiveStatus;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Finance_Card")
@Data
@ToString
@Builder
public class CardDbDto {
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
    private Integer repaymentDay;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private ActiveStatus activeStatus;
    private String cardNumber;
    private Integer closingDay;
    private String imageUrl;
}