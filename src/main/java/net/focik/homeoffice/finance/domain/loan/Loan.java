package net.focik.homeoffice.finance.domain.loan;

import lombok.*;
import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.domain.payment.FinancialTransaction;
import net.focik.homeoffice.finance.domain.payment.Payment;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentType;
import org.javamoney.moneta.Money;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Loan implements FinancialTransaction {

    private int id;
    private Bank bank;
    private int idUser;
    private String name;
    private Money amount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String loanNumber;
    private String accountNumber;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate firstPaymentDate;
    private Integer numberOfInstallments;
    private Money installmentAmount;
    private PaymentStatus loanStatus;
    private Money loanCost;//prowizja itp
    private String otherInfo;
    private List<LoanInstallment> installments;

    public void addLoanInstallment(List<LoanInstallment> loanInstallments) {
        this.installments = loanInstallments;
    }

    public void changeLoanStatus(PaymentStatus loanStatus) {
        this.loanStatus = loanStatus;
    }

    @Override
    public Payment getPayment() {
        return Payment.builder()
                .id(id)
                .idUser(idUser)
                .name(name)
                .paymentDay(firstPaymentDate.getDayOfMonth())
                .paymentType(PaymentType.LOAN)
                .installments(installments)
                .build();
    }
}