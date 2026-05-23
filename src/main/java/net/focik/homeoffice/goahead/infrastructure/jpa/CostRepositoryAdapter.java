package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.goahead.domain.cost.port.secondary.CostRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.CostDbDto;
import net.focik.homeoffice.goahead.infrastructure.mapper.JpaCostMapper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CostRepositoryAdapter implements CostRepository {

    private final CostDtoRepository costDtoRepository;
    private final JpaCostMapper mapper;

    @Override
    public Cost addCost(Cost cost) {
        CostDbDto dbDto = mapper.toDto(cost);
        if (dbDto.getIdCost() != null && dbDto.getIdCost() == 0) {
            dbDto.setIdCost(null);
        }
        if (dbDto.getCostItems() != null) {
            dbDto.getCostItems().forEach(item -> {
                item.setCost(dbDto);
                if (item.getIdCostItem() != null && item.getIdCostItem() == 0) {
                    item.setIdCostItem(null);
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
        return addCost(cost);
    }

    @Override
    public void deleteCost(int id) {
        costDtoRepository.deleteById(id);
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
    public Page<Cost> findAll(Pageable pageable, String globalFilter, Integer idSupplier, LocalDate sellDate, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        Specification<CostDbDto> spec = Specification.where(null);

        if (globalFilter != null && !globalFilter.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("number")), "%" + globalFilter.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("supplier").get("name")), "%" + globalFilter.toLowerCase() + "%")
                    )
            );
        }

        if (idSupplier != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("supplier").get("id"), idSupplier));
        }

        if (sellDate != null) {
            spec = spec.and(getSpecificationByDate(sellDate, dateComparisonType));
        }

        if (status != null && status != PaymentStatus.ALL) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentStatus"), status));
        }

        return costDtoRepository.findAll(spec, pageable)
                .map(mapper::toDomain);
    }

    private Specification<CostDbDto> getSpecificationByDate(LocalDate date, String dateComparisonType) {
        return (root, query, cb) -> switch (dateComparisonType) {
            case "AFTER" -> cb.greaterThan(root.get("sellDate"), date);
            case "BEFORE" -> cb.lessThan(root.get("sellDate"), date);
            default -> cb.equal(root.get("sellDate"), date);
        };
    }
}
