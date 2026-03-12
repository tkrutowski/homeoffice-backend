package net.focik.homeoffice.goahead.domain.invoice.port.primary;

import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.SendKsefInvoiceInfoResponse;
import net.focik.homeoffice.utils.share.PaymentStatus;

import java.util.List;

public interface UpdateInvoiceUseCase {
    Invoice updateInvoice(Invoice invoice);

    void updatePaymentStatus(Integer id, PaymentStatus paymentStatus);

    SendKsefInvoiceInfoResponse sendInvoicesToKsef(List<Integer> invoicesIds);
}
