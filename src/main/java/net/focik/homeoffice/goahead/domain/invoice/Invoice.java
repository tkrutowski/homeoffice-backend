package net.focik.homeoffice.goahead.domain.invoice;

import jakarta.persistence.Transient;
import lombok.*;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.exception.InvoiceItemNotFoundException;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentMethod;
import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Invoice {
    private int idInvoice;
    private String invoiceNumber;
    private PaymentMethod paymentMethod;
    private LocalDate sellDate;//data sprzedaży
    private LocalDate invoiceDate;//data faktury
    private PaymentStatus paymentStatus;
    private LocalDate paymentDate;//termin zapłaty
    private String otherInfo;
    private List<InvoiceItem> invoiceItems;
    private Customer customer;

    public void changePaymentStatus(PaymentStatus newPaymentStatus) {
        this.paymentStatus = newPaymentStatus;
    }

    @Transient
    public Money getAmountSum() {
        Optional<Money> reduce = invoiceItems.stream()
                .map(invoiceItem -> invoiceItem.getAmount().multiply(invoiceItem.getQuantity()))
                .reduce(Money::add);
        return reduce.orElseThrow(() -> new InvoiceItemNotFoundException("Error during getAmountSum"));
    }
}
