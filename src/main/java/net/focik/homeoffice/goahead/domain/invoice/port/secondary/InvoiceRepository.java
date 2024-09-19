package net.focik.homeoffice.goahead.domain.invoice.port.secondary;

import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.InvoiceItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    void deleteInvoice(Integer id);
//
    List<Invoice> findAll();
//
    Optional<Invoice> findById(Integer id);
//
//    Optional<Invoice> findByNip(String nip);
//
    Optional<Invoice> findByNumber(String number);
//
//    List<Invoice> findAllByActive(boolean isActive);

    List<InvoiceItem> findByInvoiceId(Integer idInvoice);

    void removeInvoiceItem(Long id);
    void deleteAllInvoiceItemsByInvoiceId(Integer id);
}
