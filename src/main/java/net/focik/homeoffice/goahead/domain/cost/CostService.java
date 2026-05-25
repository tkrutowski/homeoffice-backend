package net.focik.homeoffice.goahead.domain.cost;

import jakarta.transaction.Transactional;
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
        return costRepository.saveCost(cost);
    }

    @Transactional
    public Cost getCost(int id) {
        return costRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cost not found with id: " + id));
    }

    @Transactional
    public Cost getCostForUpdate(int id) {
        return costRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cost not found with id: " + id));
    }

    public List<Cost> getAllCosts() {
        return costRepository.findAll();
    }

    public List<Cost> findBySellDateBetween(LocalDate from, LocalDate to) {
        return costRepository.findBySellDateBetween(from, to);
    }

    public Page<Cost> findCostsPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, Integer idSupplier, LocalDate sellDate, String dateComparisonType, LocalDate invoiceDate, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        return costRepository.findAll(pageable, globalFilter, idSupplier, sellDate, dateComparisonType, invoiceDate, amount, amountComparisonType, status);
    }

    @Transactional
    public Cost updateCost(Cost cost) {
        Cost existing = getCostForUpdate(cost.getId());
        return costRepository.updateCost(mergeChanges(existing, cost));
    }

    private Cost mergeChanges(Cost existing, Cost updates) {
        existing.setNumber(updates.getNumber());
        existing.setSupplier(updates.getSupplier());
        existing.setInvoiceDate(updates.getInvoiceDate());
        existing.setSellDate(updates.getSellDate());
        existing.setPaymentDate(updates.getPaymentDate());
        existing.setOtherInfo(updates.getOtherInfo());
        existing.setPaymentMethod(updates.getPaymentMethod());
        existing.setPaymentStatus(updates.getPaymentStatus());
        existing.setPdfUrl(updates.getPdfUrl());
        existing.setKsefNumber(updates.getKsefNumber());
        existing.setInvoiceHash(updates.getInvoiceHash());
        existing.setCostItems(updates.getCostItems());
        return existing;
    }

    @Transactional
    public void updatePaymentStatus(Integer id, PaymentStatus status) {
        Cost cost = getCost(id);
        cost.changePaymentStatus(status);
        costRepository.updateCost(cost);
    }

    public void deleteCost(int id) {
        costRepository.deleteCost(id);
    }

    public boolean existsByKsefNumber(String ksefNumber) {
        return costRepository.existsByKsefNumber(ksefNumber);
    }
}
