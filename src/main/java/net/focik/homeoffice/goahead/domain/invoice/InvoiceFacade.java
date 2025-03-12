package net.focik.homeoffice.goahead.domain.invoice;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.AddInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.DeleteInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.GetInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.UpdateInvoiceUseCase;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class InvoiceFacade implements UpdateInvoiceUseCase, DeleteInvoiceUseCase, AddInvoiceUseCase, GetInvoiceUseCase {

    private final InvoiceService invoiceService;

    public Invoice addInvoice(Invoice invoice) {
        return invoiceService.saveInvoice(invoice);
    }

    public Invoice findById(Integer id) {
        return invoiceService.findById(id);
    }

    public int getNewInvoiceNumber(int year) {
        return invoiceService.getNewInvoiceNumber(year);
    }

    public List<Invoice> findAllBy(PaymentStatus status) {
        return invoiceService.findByAll(status);
    }

    @Override
    public Invoice updateInvoice(Invoice invoice) {
        return invoiceService.updateInvoice(invoice);
    }

    @Override
    public void updatePaymentStatus(Integer id, PaymentStatus paymentStatus) {
        invoiceService.updatePaymentStatus(id, paymentStatus);
    }

    @Override
    public void deleteInvoice(Integer idInvoice) {
        invoiceService.deleteInvoice(idInvoice);
    }
}
