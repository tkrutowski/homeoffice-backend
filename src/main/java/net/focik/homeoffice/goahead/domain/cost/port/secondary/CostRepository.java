package net.focik.homeoffice.goahead.domain.cost.port.secondary;

import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public interface CostRepository {

    Cost saveCost(Cost cost);

    Cost updateCost(Cost cost);

    void deleteCost(int id);

    List<Cost> findAll();

    Optional<Cost> findById(int id);

    List<Cost> findByDate(LocalDate date);

    List<Cost> findBySellDateBetween(LocalDate from, LocalDate to);

    Page<Cost> findAll(Pageable pageable, String globalFilter, Integer idSupplier, LocalDate sellDate,
                       String dateComparisonType, LocalDate invoiceDate, BigDecimal amount, String amountComparisonType,
                       PaymentStatus status);

    boolean existsByKsefNumber(String ksefNumber);
}
