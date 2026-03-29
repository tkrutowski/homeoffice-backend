package net.focik.homeoffice.goahead.domain.cost;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.cost.port.primary.AddCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.DeleteCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.UpdateCostUseCase;
import net.focik.homeoffice.goahead.domain.invoice.KsefService;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import pl.akmf.ksef.sdk.client.model.invoice.InvoiceQuerySubjectType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@AllArgsConstructor
public class CostFacade implements AddCostUseCase, GetCostUseCase, UpdateCostUseCase, DeleteCostUseCase {

    private final CostService costService;
    private final KsefService ksefService;
    private final KsefCostMapper ksefCostMapper;

    @Override
    public Cost addCost(Cost cost) {
        return costService.addCost(cost);
    }

    @Override
    public Cost getCost(int id) {
        return costService.getCost(id);
    }

    @Override
    public List<Cost> getAllCosts() {
        return costService.getAllCosts();
    }

    @Override
    public List<Cost> getCostsByDate(LocalDate date) {
        return costService.getCostsByDate(date);
    }

    @Override
    public Page<Cost> findCostsPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, Integer idSeller, LocalDate sellDate, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        return costService.findCostsPageableWithFilters(page, size, sortField, sortDirection, globalFilter, idSeller, sellDate, dateComparisonType, amount, amountComparisonType, status);
    }

    @Override
    public List<Cost> findKsefCosts(LocalDate fromDate, LocalDate toDate) {
        List<InvoiceKsefDto> invoices = ksefService.findInvoices(fromDate, toDate, InvoiceQuerySubjectType.SUBJECT2);
        return invoices.stream().map(ksefCostMapper::toCost).toList();
    }

    @Override
    public Cost updateCost(Cost cost) {
        return costService.updateCost(cost);
    }

    @Override
    public void deleteCost(int id) {
        costService.deleteCost(id);
    }
}
