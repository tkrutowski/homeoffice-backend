package net.focik.homeoffice.goahead.domain.invoice.port.secondary;

import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    void deleteInvoice(Integer id);

    List<Invoice> findAll();

    Optional<Invoice> findById(Integer id);

    Optional<Invoice> findByNumber(String number);

    Page<Invoice> findAll(Pageable pageable, String globalFilter, Integer idCustomer, LocalDate sellDate,
                          String sellDateComparisonType, BigDecimal amount, String amountComparisonType,
                          PaymentStatus status);

    List<Invoice> findLastInvoiceNumberByYear(Integer year);
}
