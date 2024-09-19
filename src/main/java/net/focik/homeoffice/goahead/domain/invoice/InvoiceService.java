package net.focik.homeoffice.goahead.domain.invoice;

import lombok.AllArgsConstructor;
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

@Service
@AllArgsConstructor
class InvoiceService {

    InvoiceRepository invoiceRepository;
    ICustomerService customerService;

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        validate(invoice);
        updateAmount(invoice);

        return invoiceRepository.save(invoice);
    }

    private void validate(Invoice invoice) {
        Optional<Invoice> byNumber = invoiceRepository.findByNumber(invoice.getInvoiceNumber());
        if (byNumber.isPresent())
            throw new InvoiceAlreadyExistException("Faktura o numerze " + invoice.getInvoiceNumber() + " już istnieje.");
//        if (fin.getCustomerType() == CustomerType.COMPANY) {
//            Optional<Customer> byNip = invoiceRepository.findByNip(invoice.getNip());
//            if (byNip.isPresent()) {
//                throw new CustomerAlreadyExistException("Klient o NIP-ie " + invoice.getNip() + "już istnieje.");
//            }
//        }
    }

    public Invoice findById(Integer id) {
        Optional<Invoice> byId = invoiceRepository.findById(id);
        if (byId.isEmpty()) {
            throw new InvoiceNotFoundException("Invoice with id: " + id + " not found.");
        }
        byId.get().setInvoiceItems(findAllByInvoiceId(id));
        return byId.get();
    }

    public List<Invoice> findByAll(PaymentStatus paymentStatus, Boolean isGetItems, Boolean isGetCustomer) {
        List<Invoice> invoiceList = invoiceRepository.findAll();

        if (paymentStatus != null && paymentStatus != PaymentStatus.ALL) {
            invoiceList = invoiceList.stream()
                    .filter(invoice -> paymentStatus.equals(invoice.getPaymentStatus()))
                    .collect(Collectors.toList());
        }


        if (isGetItems != null && isGetItems) {
            invoiceList
                    .forEach(invoice -> invoice.setInvoiceItems(findAllByInvoiceId(invoice.getIdInvoice())));
        }

        if (isGetCustomer != null && isGetCustomer) {
            invoiceList.forEach(invoice -> invoice.setCustomer(customerService.findById(invoice.getCustomer().getId(), false)));
        }

        return invoiceList;
    }

    public int getNewInvoiceNumber(int year) {
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
        Invoice byId = findById(id);
        byId.setCustomer(customerService.findById(byId.getCustomer().getId(), true));
        return byId;
    }

    public void updatePaymentStatus(Integer id, PaymentStatus status) {
        Invoice invoice = findById(id);
        invoice.changePaymentStatus(status);

        invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateInvoice(Invoice invoice) {
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
        List<InvoiceItem> invoiceItems = invoice.getInvoiceItems();
        CurrencyUnit currencyUnit = Monetary.getCurrency("PLN");
        if (invoiceItems != null) {
            invoice.setAmount(invoiceItems.stream()
                    .map(invoiceItem -> invoiceItem.getAmount().multiply(invoiceItem.getQuantity()))
                    .reduce((money, money2) -> money2.add(money))
                    .orElse(Money.zero(currencyUnit)));
        }
    }

    @Transactional
    public void deleteInvoice(Integer idInvoice) {
        invoiceRepository.deleteAllInvoiceItemsByInvoiceId(idInvoice);
        invoiceRepository.deleteInvoice(idInvoice);
    }
}
