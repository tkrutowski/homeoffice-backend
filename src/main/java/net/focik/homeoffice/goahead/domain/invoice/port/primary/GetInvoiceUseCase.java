package net.focik.homeoffice.goahead.domain.invoice.port.primary;

import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.utils.share.PaymentStatus;

import java.util.List;

public interface GetInvoiceUseCase {
    Invoice findById(Integer id);

    int getNewInvoiceNumber(int year);

    List<Invoice> findAllBy(PaymentStatus status);
}
