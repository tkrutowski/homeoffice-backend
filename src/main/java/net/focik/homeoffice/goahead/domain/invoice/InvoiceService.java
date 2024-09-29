package net.focik.homeoffice.goahead.domain.invoice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.customer.ICustomerService;
import net.focik.homeoffice.goahead.domain.exception.InvoiceAlreadyExistException;
import net.focik.homeoffice.goahead.domain.exception.InvoiceNotFoundException;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
class InvoiceService {

    InvoiceRepository invoiceRepository;
    ICustomerService customerService;

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        log.debug("Trying to save invoice {}", invoice);
        validate(invoice);
        updateAmount(invoice);

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
        byId.get().setInvoiceItems(findAllByInvoiceId(id));
        log.debug("Found invoice with id {}", id);
        return byId.get();
    }

    public List<Invoice> findByAll(PaymentStatus paymentStatus, Boolean isGetItems, Boolean isGetCustomer) {
        log.debug("Trying to find invoice with payment status {}, isCustomer = {}, isItems = {}", paymentStatus, isGetCustomer, isGetItems);
        List<Invoice> invoiceList = invoiceRepository.findAll();

        if (paymentStatus != null && paymentStatus != PaymentStatus.ALL) {
            log.debug("Found invoice with payment status {}", paymentStatus);
            invoiceList = invoiceList.stream()
                    .filter(invoice -> paymentStatus.equals(invoice.getPaymentStatus()))
                    .collect(Collectors.toList());
        }

        if (isGetItems != null && isGetItems) {
            log.debug("Adding invoiceItems for found invoices");
            invoiceList
                    .forEach(invoice -> invoice.setInvoiceItems(findAllByInvoiceId(invoice.getIdInvoice())));
        }

        if (isGetCustomer != null && isGetCustomer) {
            log.debug("Adding customers for found invoices");
            invoiceList.forEach(invoice -> invoice.setCustomer(customerService.findById(invoice.getCustomer().getId(), false)));
        }

        return invoiceList;
    }

    public int getNewInvoiceNumber(int year) {
        log.debug("Trying to get new invoice number for year {}", year);
        int latestNumber = findByAll(null, null, null).stream()
                .map(Invoice::getInvoiceNumber)
                .map(s -> s.split("/"))
                .filter(strings -> Integer.parseInt(strings[0]) == year)
                .mapToInt(value -> Integer.parseInt(value[1]))
                .max()
                .orElse(0);
        return ++latestNumber;
    }

    public List<InvoiceItem> findAllByInvoiceId(int invoiceId) {
        return invoiceRepository.findByInvoiceId(invoiceId);
    }

    public Invoice findFullById(Integer id) {
        log.debug("Trying to find invoice with id {}", id);
        Invoice byId = findById(id);
        byId.setCustomer(customerService.findById(byId.getCustomer().getId(), true));
        return byId;
    }

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
        Invoice byId = findById(invoice.getIdInvoice());
        invoiceRepository.deleteAllInvoiceItemsByInvoiceId(byId.getIdInvoice());
        byId.setCustomer(invoice.getCustomer());
        byId.setInvoiceNumber(invoice.getInvoiceNumber());
        byId.setInvoiceDate(invoice.getInvoiceDate());
        byId.setSellDate(invoice.getSellDate());
        byId.setOtherInfo(invoice.getOtherInfo());
        byId.setPaymentDate(invoice.getPaymentDate());
        byId.setPaymentMethod(invoice.getPaymentMethod());
        byId.setInvoiceItems(invoice.getInvoiceItems());

        updateAmount(invoice);


        return invoiceRepository.save(invoice);
    }

    private void updateAmount(Invoice invoice) {
        log.debug("Trying to update amount base on invoice: {}", invoice);
        List<InvoiceItem> invoiceItems = invoice.getInvoiceItems();
        CurrencyUnit currencyUnit = Monetary.getCurrency("PLN");
        if (invoiceItems != null) {
            invoice.setAmount(invoiceItems.stream()
                    .map(invoiceItem -> invoiceItem.getAmount().multiply(invoiceItem.getQuantity()))
                    .reduce((money, money2) -> money2.add(money))
                    .orElse(Money.zero(currencyUnit)));
            log.debug("Updated invoice amount: {}", invoice.getAmount());
        }
    }

    @Transactional
    public void deleteInvoice(Integer idInvoice) {
        log.debug("Trying to delete invoice with id {}", idInvoice);
        invoiceRepository.deleteAllInvoiceItemsByInvoiceId(idInvoice);
        invoiceRepository.deleteInvoice(idInvoice);
        log.debug("Deleted invoice with id {}", idInvoice);
    }
}
