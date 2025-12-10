package net.focik.homeoffice.goahead.domain.invoice.port.primary;

import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface GetInvoiceUseCase {
    Invoice findById(Integer id);

    int getNewInvoiceNumber(int year);

    Page<Invoice> findInvoicesPageableWithFilters(int page, int size, String sortField, String sortDirection,
                                                  String globalFilter, Integer idCustomer, LocalDate date,
                                                  String dateComparisonType, BigDecimal amount,
                                                  String amountComparisonType, PaymentStatus status);
    String sendInvoiceToS3(int invoice);

    Map<Integer, List<BigDecimal>> getStatistic();
    Map<Integer, List<BigDecimal>> getStatisticByCustomer(Integer year);
}
