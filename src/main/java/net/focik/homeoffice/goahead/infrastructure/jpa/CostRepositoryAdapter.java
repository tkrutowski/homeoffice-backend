package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.goahead.domain.cost.port.secondary.CostRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.CostDbDto;
import net.focik.homeoffice.goahead.infrastructure.mapper.JpaCostMapper;
import net.focik.homeoffice.utils.JpaSpecificationHelper;
import net.focik.homeoffice.utils.share.PaymentStatus;
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
import net.focik.homeoffice.goahead.infrastructure.dto.CostItemDbDto;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CostRepositoryAdapter implements CostRepository {

    private final CostDtoRepository costDtoRepository;
    private final JpaCostMapper mapper;

    @Override
    public Cost saveCost(Cost cost) {
        CostDbDto dbDto = mapper.toDto(cost);
        if (dbDto.getId() != null && dbDto.getId() == 0) {
            dbDto.setId(null);
        }
        if (dbDto.getCostItems() != null) {
            dbDto.getCostItems().forEach(item -> {
                item.setCost(dbDto);
                if (item.getId() != null && item.getId() == 0) {
                    item.setId(null);
                }
            });
        }
        if (dbDto.getKsefNumber() != null && dbDto.getKsefNumber().trim().isEmpty()) {
            dbDto.setKsefNumber(null);
        }
        
        CostDbDto saved = costDtoRepository.save(dbDto);
        return mapper.toDomain(saved);
    }

    @Override
    public Cost updateCost(Cost cost) {
        return saveCost(cost);
    }

    @Override
    public void deleteCost(int id) {
        costDtoRepository.findById(id)
                .ifPresent(costDtoRepository::delete);
    }

    @Override
    public List<Cost> findAll() {
        return costDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Cost> findById(int id) {
        return costDtoRepository.findById(id)
                 .map(mapper::toDomain);
    }

    @Override
    public List<Cost> findByDate(LocalDate date) {
        return costDtoRepository.findByInvoiceDate(date).stream()
                 .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Cost> findBySellDateBetween(LocalDate from, LocalDate to) {
        return costDtoRepository.findBySellDateBetween(from, to).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Cost> findAll(Pageable pageable, String globalFilter, Integer idSupplier, LocalDate sellDate, String dateComparisonType, LocalDate invoiceDate, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        // Spring Data JPA 4.0+: Specification.where(null) rzuca IllegalArgumentException
        // ("Specification must not be null") - trzeba użyć unrestricted() jako punktu startowego.
        Specification<CostDbDto> spec = Specification.unrestricted();

        if (globalFilter != null && !globalFilter.isEmpty()) {
            spec = spec.and((root, _, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("number")), "%" + globalFilter.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("supplier").get("name")), "%" + globalFilter.toLowerCase() + "%")
                    )
            );
        }

        if (idSupplier != null) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("supplier").get("id"), idSupplier));
        }

        if (sellDate != null) {
            spec = spec.and(JpaSpecificationHelper.byDate(sellDate, dateComparisonType, "sellDate"));
        }

        if (invoiceDate != null) {
            spec = spec.and(JpaSpecificationHelper.byDate(invoiceDate, dateComparisonType, "invoiceDate"));
        }
        if (amount != null) {
            spec = spec.and((root, query, cb) -> {
                Expression<BigDecimal> total = costTotal(root, query, cb);
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
                BigDecimal tolerance = new BigDecimal("0.005");
                return cb.between(total, amount.subtract(tolerance), amount.add(tolerance));
            });
        }

        if (status != null && status != PaymentStatus.ALL) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("paymentStatus"), status));
        }

        // "amount" nie jest kolumną kosztu - to suma amountGross pozycji, więc sortowanie budujemy ręcznie.
        if (pageable.getSort().stream().anyMatch(o -> "amount".equals(o.getProperty()))) {
            Sort sort = pageable.getSort();
            spec = spec.and((root, query, cb) -> {
                if (!Long.class.equals(query.getResultType())) {
                    List<Order> orders = new ArrayList<>();
                    for (Sort.Order o : sort) {
                        Expression<?> expr = "amount".equals(o.getProperty())
                                ? costTotal(root, query, cb)
                                : root.get(o.getProperty());
                        orders.add(o.isAscending() ? cb.asc(expr) : cb.desc(expr));
                    }
                    query.orderBy(orders);
                }
                return null;
            });
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        return costDtoRepository.findAll(spec, pageable)
                .map(mapper::toDomain);
    }

    /** Kwota brutto kosztu = suma amountGross jego pozycji, jako podzapytanie. */
    private Expression<BigDecimal> costTotal(Root<CostDbDto> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<BigDecimal> sub = query.subquery(BigDecimal.class);
        Root<CostItemDbDto> item = sub.from(CostItemDbDto.class);
        sub.select(cb.sum(item.get("amountGross")))
                .where(cb.equal(item.get("cost"), root));
        return sub;
    }

    @Override
    public boolean existsByKsefNumber(String ksefNumber) {
        return costDtoRepository.existsByKsefNumber(ksefNumber);
    }

    @Override
    public boolean existsBySupplier(Integer idSupplier) {
        return costDtoRepository.existsBySupplierId(idSupplier);
    }
}
