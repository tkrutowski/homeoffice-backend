package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final InvoiceDtoRepository invoiceDtoRepository;
    private final ModelMapper mapper;

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceDbDto dbDto = mapper.map(invoice, InvoiceDbDto.class);
        if (dbDto.getIdInvoice() == 0){
            dbDto.setIdInvoice(null);
        }
        if (dbDto.getInvoiceItems() != null) {
            dbDto.getInvoiceItems().forEach(invoiceItemDto -> {
                if (invoiceItemDto.getIdInvoiceItem() == 0) {
                    invoiceItemDto.setIdInvoiceItem(null);
                }
                invoiceItemDto.setInvoice(dbDto);
            });
        }
        InvoiceDbDto saved = invoiceDtoRepository.save(dbDto);
        return mapper.map(saved, Invoice.class);
    }

    @Override
    public void deleteInvoice(Integer id) {
        invoiceDtoRepository.deleteById(id);
    }

    @Override
    public List<Invoice> findAll() {
        List<InvoiceDbDto> all = invoiceDtoRepository.findAll();
        return all.stream()
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findById(Integer id) {
        return invoiceDtoRepository.findById(id)
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class));
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return invoiceDtoRepository.findByNumber(number)
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class));
    }

    @Override
    public Page<Invoice> findAll(Pageable pageable, String globalFilter, Integer idCustomer, LocalDate sellDate, String sellDateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        Specification<InvoiceDbDto> spec = Specification.where(null);

        if (globalFilter != null && !globalFilter.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("number")), "%" + globalFilter.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("customer").get("name")), "%" + globalFilter.toLowerCase() + "%")
                    )
            );
        }

        if (idCustomer != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), idCustomer));
        }

        if (sellDate != null) {
            spec = spec.and(getSpecificationByDate(sellDate, sellDateComparisonType));
        }

//        if (amount != null) {
//            spec = spec.and(getSpecificationByAmount(amount, amountComparisonType));
//        }

        if (status != null && status != PaymentStatus.ALL) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentStatus"), status));
        }

        return invoiceDtoRepository.findAll(spec, pageable)
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class));
    }

    @Override
    public List<Invoice> findLastInvoiceNumberByYear(Integer year) {
        return invoiceDtoRepository.findInvoiceDbDtosByNumberContainsOrderByNumberDesc(year.toString()).stream()
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, List<BigDecimal>> getStatistic() {
        Map<Integer, List<BigDecimal>> result = new HashMap<>();

        List<Object[]> monthlyStats = invoiceDtoRepository.findByMonthlyAmountStats();

        for (Object[] stat : monthlyStats) {
            Integer year = (Integer) stat[0];
            Integer month = (Integer) stat[1];
            BigDecimal total = BigDecimal.valueOf((double)stat[2]);

            if (!result.containsKey(year)) {
                result.put(year, new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO)));
            }

            result.get(year).set(month - 1, total);
        }

        return result;
    }

    @Override
    public Map<Integer, List<BigDecimal>> getMonthlyStatisticsByYearAndCustomer(Integer year) {
        Map<Integer, List<BigDecimal>> result = new HashMap<>();

        List<Object[]> customerMonthlyStats = invoiceDtoRepository.findByMonthlyAmountStatsByYearAndCustomer(year);

        for (Object[] stat : customerMonthlyStats) {
            Integer customerId = (Integer) stat[0];
            Integer month = (Integer) stat[1];
            BigDecimal total = BigDecimal.valueOf((double) stat[2]);

            // Inicjalizujemy listę dla klienta jeśli jeszcze nie istnieje
            if (!result.containsKey(customerId)) {
                result.put(customerId, new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO)));
            }

            // Ustawiamy wartość dla danego miesiąca (indeks 0-based)
            result.get(customerId).set(month - 1, total);
        }

        return result;
    }

    private Specification<InvoiceDbDto> getSpecificationByDate(LocalDate date, String dateComparisonType) {
        return (root, query, cb) -> switch (dateComparisonType) {
            case "AFTER" -> cb.greaterThan(root.get("sellDate"), date);
            case "BEFORE" -> cb.lessThan(root.get("sellDate"), date);
            default -> cb.equal(root.get("sellDate"), date);
        };
    }

    private Specification<InvoiceDbDto> getSpecificationByAmount(BigDecimal amount, String amountComparisonType) {
        return (root, query, cb) -> {
            switch (amountComparisonType) {
                case "GREATER":
                    return cb.greaterThan(root.get("grossAmount"), amount);
                case "LESS":
                    return cb.lessThan(root.get("grossAmount"), amount);
                default:
                    return cb.equal(root.get("grossAmount"), amount);
            }
        };
    }
}
