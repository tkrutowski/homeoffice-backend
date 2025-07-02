package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Finance_Loan_Installment")
@Getter
@Setter
@ToString
@Builder
public class LoanInstallmentDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer idLoan;
    private Integer installmentNumber;
    @Column(name = "amount_to_pay")
    private BigDecimal installmentAmountToPay;
    @Column(name = "amount_paid")
    private BigDecimal installmentAmountPaid;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDeadline;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
