package net.focik.homeoffice.goahead.domain.cost;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.cost.port.secondary.CostRepository;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
class CostService {

    private final CostRepository costRepository;

    public Cost addCost(Cost cost) {
        return costRepository.addCost(cost);
    }

    public Cost getCost(int id) {
        return costRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cost not found with id: " + id));
    }

    public List<Cost> getAllCosts() {
        return costRepository.findAll();
    }

    public List<Cost> getCostsByDate(LocalDate date) {
        return costRepository.findByDate(date);
    }

    public Page<Cost> findCostsPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, Integer idSeller, LocalDate sellDate, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        return costRepository.findAll(pageable, globalFilter, idSeller, sellDate, dateComparisonType, amount, amountComparisonType, status);
    }

    public Cost updateCost(Cost cost) {
        return costRepository.updateCost(cost);
    }

    public void deleteCost(int id) {
        costRepository.deleteCost(id);
    }
}
