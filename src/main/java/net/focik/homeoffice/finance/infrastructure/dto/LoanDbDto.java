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
@Table(name = "Finance_Loan")
@Getter
@Setter
@ToString
@Builder
public class LoanDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_bank")
    private BankDbDto bank;
    private Integer idUser;
    private BigDecimal amount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String loanNumber;
    private String accountNumber;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate firstPaymentDate;
    private Integer numberOfInstallments;
    private BigDecimal installmentAmount;
    private String name;
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private PaymentStatus loanStatus;
    private BigDecimal loanCost;
}
