package net.focik.homeoffice.goahead.domain.invoice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.exception.InvoiceAlreadyExistException;
import net.focik.homeoffice.goahead.domain.exception.InvoiceNotFoundException;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
class InvoiceService {

    InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        log.debug("Trying to save invoice {}", invoice);
        validate(invoice);
        return invoiceRepository.save(invoice);
    }

    private void validate(Invoice invoice) {
        log.debug("Validating invoice {}", invoice);
        Optional<Invoice> byNumber = invoiceRepository.findByNumber(invoice.getInvoiceNumber());
        if (byNumber.isPresent())
            throw new InvoiceAlreadyExistException("Faktura o numerze " + invoice.getInvoiceNumber() + " już istnieje.");
    }

    public Invoice findById(Integer id) {
        log.debug("Trying to find invoice with id {}", id);
        Optional<Invoice> byId = invoiceRepository.findById(id);
        if (byId.isEmpty()) {
            throw new InvoiceNotFoundException("Invoice with id: " + id + " not found.");
        }
        log.debug("Found invoice with id {}", id);
        return byId.get();
    }

    public List<Invoice> findAllBy(PaymentStatus paymentStatus) {
        log.debug("Trying to find invoice with payment status {}", paymentStatus);
        List<Invoice> invoiceList = invoiceRepository.findAll();

        if (paymentStatus != null && paymentStatus != PaymentStatus.ALL) {
            log.debug("Found invoice with payment status {}", paymentStatus);
            invoiceList = invoiceList.stream()
                    .filter(invoice -> paymentStatus.equals(invoice.getPaymentStatus()))
                    .collect(Collectors.toList());
        }
        return invoiceList;
    }

    public int getNewInvoiceNumber(int year) {
        log.info("Trying to get new invoice number for year " + year);
        int latestNumber = invoiceRepository.findLastInvoiceNumberByYear(year).stream()
                .map(Invoice::getInvoiceNumber)
                .map(s -> s.split("/"))
                .filter(strings -> Integer.parseInt(strings[0]) == year)
                .mapToInt(value -> Integer.parseInt(value[1]))
                .max()
                .orElse(0);
        return ++latestNumber;
    }

    public Page<Invoice> findInvoicesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, Integer idCustomer, LocalDate sellDate, String sellDateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return invoiceRepository.findAll(pageRequest, globalFilter, idCustomer, sellDate, sellDateComparisonType, amount, amountComparisonType, status);
    }

    @Transactional
    public void updatePaymentStatus(Integer id, PaymentStatus status) {
        log.debug("Trying to update payment status with id {}, new status: {}", id, status);
        Invoice invoice = findById(id);
        invoice.changePaymentStatus(status);

        invoiceRepository.save(invoice);
        log.debug("Updated invoice status with id {}", id);
    }

    @Transactional
    public Invoice updateInvoice(Invoice invoice) {
        log.debug("Trying to update invoice with: {}", invoice);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteInvoice(Integer idInvoice) {
        log.debug("Trying to delete invoice with id {}", idInvoice);
        invoiceRepository.deleteInvoice(idInvoice);
        log.debug("Deleted invoice with id {}", idInvoice);
    }

    public Map<Integer, List<BigDecimal>> getStatistic() {
        return invoiceRepository.getStatistic();
    }

    public Map<Integer, List<BigDecimal>> getMonthlyStatisticsByYearAndCustomer(Integer year) {
        return invoiceRepository.getMonthlyStatisticsByYearAndCustomer(year);
    }
}