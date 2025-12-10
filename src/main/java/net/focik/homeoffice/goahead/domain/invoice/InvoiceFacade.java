package net.focik.homeoffice.goahead.domain.invoice;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.AddInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.DeleteInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.GetInvoiceUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.UpdateInvoiceUseCase;
import net.focik.homeoffice.utils.FileHelperS3;
import net.focik.homeoffice.utils.share.Module;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class InvoiceFacade implements UpdateInvoiceUseCase, DeleteInvoiceUseCase, AddInvoiceUseCase, GetInvoiceUseCase {

    private final InvoiceService invoiceService;
    private final FileHelperS3 fileHelperS3;

    public Invoice addInvoice(Invoice invoice) {
        return invoiceService.saveInvoice(invoice);
    }

    public Invoice findById(Integer id) {
        return invoiceService.findById(id);
    }

    public int getNewInvoiceNumber(int year) {
        return invoiceService.getNewInvoiceNumber(year);
    }

    @Override
    public Page<Invoice> findInvoicesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, Integer idCustomer, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        return invoiceService.findInvoicesPageableWithFilters(page, size, sortField, sortDirection, globalFilter, idCustomer, date, dateComparisonType, amount, amountComparisonType, status);
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

    @Override
    public String sendInvoiceToS3(int idInvoice) {
        Invoice invoice = findById(idInvoice);
        String filePath = InvoicePdf.createPdf(invoice);
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        String s3Url = fileHelperS3.saveInBucket(file, Module.GO_AHEAD);
        file.delete();
        return s3Url;
    }

    @Override
    public Map<Integer, List<BigDecimal>> getStatistic() {
        return invoiceService.getStatistic();
    }

    @Override
    public Map<Integer, List<BigDecimal>> getStatisticByCustomer(Integer year) {
        return invoiceService.getMonthlyStatisticsByYearAndCustomer(year);
    }
}
