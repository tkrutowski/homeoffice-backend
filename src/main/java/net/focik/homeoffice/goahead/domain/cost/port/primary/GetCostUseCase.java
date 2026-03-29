package net.focik.homeoffice.goahead.domain.cost.port.primary;

import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GetCostUseCase {
    Cost getCost(int id);
    List<Cost> getAllCosts();
    List<Cost> getCostsByDate(LocalDate date);
    Page<Cost> findCostsPageableWithFilters(int page, int size, String sortField, String sortDirection,
                                            String globalFilter, Integer idSeller, LocalDate sellDate,
                                            String dateComparisonType, BigDecimal amount, String amountComparisonType,
                                            PaymentStatus status);
    List<Cost> findKsefCosts(LocalDate fromDate, LocalDate toDate);
}
