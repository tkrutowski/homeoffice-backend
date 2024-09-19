package net.focik.homeoffice.goahead.domain.invoice.port.primary;

import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.utils.share.PaymentStatus;

public interface UpdateInvoiceUseCase {
    Invoice updateInvoice(Invoice invoice);

    void updatePaymentStatus(Integer id, PaymentStatus paymentStatus);
}
