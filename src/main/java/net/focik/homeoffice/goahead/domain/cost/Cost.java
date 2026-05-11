package net.focik.homeoffice.goahead.domain.cost;

import lombok.*;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Cost {
    private int idCost;
    private String number;
    private PaymentMethod paymentMethod;
    private LocalDate sellDate;
    private LocalDate invoiceDate;
    private PaymentStatus paymentStatus;
    private LocalDate paymentDate;
    private String otherInfo;
    private List<CostItem> costItems;
    private Supplier supplier;
    private String ksefNumber;
    private String invoiceHash;
    private String pdfUrl;

    public void changePaymentStatus(PaymentStatus newPaymentStatus) {
        this.paymentStatus = newPaymentStatus;
    }

    public Money getAmountSum() {
        return costItems.stream()
                .map(item -> item.getAmountGross().multiply(item.getQuantity()))
                .reduce(Money.of(0, "PLN"), Money::add);
    }

    public Money getAmountNetSum() {
        return costItems.stream()
                .map(item -> item.getAmountNet().multiply(item.getQuantity()))
                .reduce(Money.of(0, "PLN"), Money::add);
    }

    public Money getAmountVatSum() {
        return costItems.stream()
                .map(item -> item.getAmountVat().multiply(item.getQuantity()))
                .reduce(Money.of(0, "PLN"), Money::add);
    }
}
