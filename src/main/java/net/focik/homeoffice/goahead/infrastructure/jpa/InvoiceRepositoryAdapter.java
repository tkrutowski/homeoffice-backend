package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.InvoiceItem;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import net.focik.homeoffice.utils.JpaSpecificationHelper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;
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
        
        if (dbDto.getId() != null && dbDto.getId() == 0) {
            dbDto.setId(null);
        }

        if (dbDto.getKsefNumber() != null && dbDto.getKsefNumber().trim().isEmpty()) {
            dbDto.setKsefNumber(null);
        }
        
        if (dbDto.getInvoiceItems() != null) {
            dbDto.getInvoiceItems().forEach(invoiceItemDto -> {
                if (invoiceItemDto.getId() != null && invoiceItemDto.getId() == 0) {
                    invoiceItemDto.setId(null);
                }
                invoiceItemDto.setInvoice(dbDto);
            });
        }

        InvoiceDbDto saved = invoiceDtoRepository.save(dbDto);
        return mapToDomain(saved);
    }

    @Override
    public void deleteInvoice(Integer id) {
        invoiceDtoRepository.findById(id).ifPresent(invoiceDtoRepository::delete);
    }

    @Override
    public List<Invoice> findAll() {
        List<InvoiceDbDto> all = invoiceDtoRepository.findAll();
        return all.stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findById(Integer id) {
        return invoiceDtoRepository.findById(id)
                .map(this::mapToDomain);
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return invoiceDtoRepository.findByNumber(number)
                .map(this::mapToDomain);
    }

    @Override
    public Page<Invoice> findAll(Pageable pageable, String globalFilter, Integer idCustomer, LocalDate sellDate, String sellDateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        // Spring Data JPA 4.0+: Specification.where(null) rzuca IllegalArgumentException
        // ("Specification must not be null") - trzeba użyć unrestricted() jako punktu startowego.
        Specification<InvoiceDbDto> spec = Specification.unrestricted();

        if (globalFilter != null && !globalFilter.isEmpty()) {
            spec = spec.and((root, _, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("number")), "%" + globalFilter.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("customer").get("name")), "%" + globalFilter.toLowerCase() + "%")
                    )
            );
        }

        if (idCustomer != null) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("customer").get("id"), idCustomer));
        }

        if (sellDate != null) {
            spec = spec.and(JpaSpecificationHelper.byDate(sellDate, sellDateComparisonType, "sellDate"));
        }

        if (amount != null) {
            spec = spec.and((root, query, cb) -> {
                Expression<BigDecimal> total = invoiceTotal(root, query, cb);
                switch (amountComparisonType) {
                    case "GREATER_THAN", "GREATER" -> {
                        return cb.greaterThan(total, amount);
                    }
                    case "GREATER_THAN_OR_EQUAL" -> {
                        return cb.greaterThanOrEqualTo(total, amount);
                    }
                    case "LESS_THAN", "LESS" -> {
                        return cb.lessThan(total, amount);
                    }
                    case "LESS_THAN_OR_EQUAL" -> {
                        return cb.lessThanOrEqualTo(total, amount);
                    }
                    default -> { }
                }
                // suma amount * quantity (Float) bywa niedokładna - równość z tolerancją grosza
                BigDecimal tolerance = new BigDecimal("0.005");
                return cb.between(total, amount.subtract(tolerance), amount.add(tolerance));
            });
        }

        if (status != null && status != PaymentStatus.ALL) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("paymentStatus"), status));
        }

        // "amount" nie jest kolumną faktury - to suma pozycji (amount * quantity), więc sortowanie
        // po niej trzeba zbudować ręcznie; Sort.by("amount") kończy się PropertyReferenceException.
        if (pageable.getSort().stream().anyMatch(o -> "amount".equals(o.getProperty()))) {
            Sort sort = pageable.getSort();
            spec = spec.and((root, query, cb) -> {
                if (!Long.class.equals(query.getResultType())) {
                    List<Order> orders = new ArrayList<>();
                    for (Sort.Order o : sort) {
                        Expression<?> expr;
                        if ("amount".equals(o.getProperty())) {
                            expr = invoiceTotal(root, query, cb);
                        } else {
                            expr = root.get(o.getProperty());
                        }
                        orders.add(o.isAscending() ? cb.asc(expr) : cb.desc(expr));
                    }
                    query.orderBy(orders);
                }
                return null;
            });
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        return invoiceDtoRepository.findAll(spec, pageable)
                .map(this::mapToDomain);
    }

    /** Kwota brutto faktury = suma (amount * quantity) jej pozycji, jako podzapytanie. */
    private Expression<BigDecimal> invoiceTotal(Root<InvoiceDbDto> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<BigDecimal> sub = query.subquery(BigDecimal.class);
        Root<InvoiceItemDbDto> item = sub.from(InvoiceItemDbDto.class);
        sub.select(cb.sum(cb.prod(item.get("amount"), item.get("quantity"))).as(BigDecimal.class))
                .where(cb.equal(item.get("invoice"), root));
        return sub;
    }

    @Override
    public List<Invoice> findLastInvoiceNumberByYear(Integer year) {
        return invoiceDtoRepository.findInvoiceDbDtosByNumberContainsOrderByNumberDesc(year.toString()).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findBySellDateBetween(LocalDate from, LocalDate to) {
        return invoiceDtoRepository.findBySellDateBetween(from, to).stream()
                .map(this::mapToDomain)
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

    @Override
    public boolean existsByCustomer(Integer idCustomer) {
        return invoiceDtoRepository.existsByCustomer_Id(idCustomer);
    }

    private Invoice mapToDomain(InvoiceDbDto dbDto) {
        Invoice invoice = mapper.map(dbDto, Invoice.class);
        if (dbDto.getInvoiceItems() != null) {
            // ModelMapper can sometimes have issues with persistent bags, we can manually map the items or copy them to a new arraylist
            List<InvoiceItem> items = dbDto.getInvoiceItems().stream()
                .map(item -> mapper.map(item, InvoiceItem.class))
                .collect(Collectors.toList());
            invoice.setInvoiceItems(items);
        }
        return invoice;
    }
}