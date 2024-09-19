package net.focik.homeoffice.goahead.domain.invoice;

import lombok.*;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentMethod;
import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.util.List;

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
    private Money amount;//brutto
    private PaymentStatus paymentStatus;
    private LocalDate paymentDate;//termin zapłaty
    private String otherInfo;
    private List<InvoiceItem> invoiceItems;
    private Customer customer;

    public void changePaymentStatus(PaymentStatus newPaymentStatus) {
        this.paymentStatus = newPaymentStatus;
    }
}
