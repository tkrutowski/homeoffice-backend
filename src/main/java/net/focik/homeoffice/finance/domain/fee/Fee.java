package net.focik.homeoffice.finance.domain.fee;

import lombok.*;
import net.focik.homeoffice.finance.domain.firm.Firm;
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
public class Fee implements FinancialTransaction {

    private Integer id;
    private Firm firm;
    private Integer idUser;
    private String name;
    private String feeNumber;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private FeeFrequencyEnum feeFrequency;
    private Integer numberOfPayments;//ile razy pobrać
    private Money amount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate firstPaymentDate;
    private String accountNumber;
    private PaymentStatus feeStatus;
    private String otherInfo;
    private List<FeeInstallment> installments;

    public void addFeeInstallment(List<FeeInstallment> feeInstallments) {
        this.installments = feeInstallments;
    }

    public void changeFeeStatus(PaymentStatus feeStatus) {
        this.feeStatus = feeStatus;
    }

    @Override
    public Payment getPayment() {
        return Payment.builder()
                .id(id)
                .idUser(idUser)
                .name(name)
                .paymentDay(firstPaymentDate.getDayOfMonth())
                .paymentType(PaymentType.FEE)
                .installments(installments)
                .build();
    }
}