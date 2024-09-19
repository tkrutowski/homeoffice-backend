package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.finance.domain.fee.FeeFrequencyEnum;
import net.focik.homeoffice.utils.share.PaymentStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Finance_Fee")
@Getter
@ToString
@Builder
public class FeeDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_firm")
    private FirmDbDto firm;
    private Integer idUser;
    private String name;
    private BigDecimal amount;
    private LocalDate date;
    private String feeNumber;
    private String accountNumber;
    private LocalDate firstPaymentDate;
    private Integer numberOfPayments;
    @Enumerated(EnumType.STRING)
    private FeeFrequencyEnum feeFrequencyEnum;
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private PaymentStatus feeStatus;
}
