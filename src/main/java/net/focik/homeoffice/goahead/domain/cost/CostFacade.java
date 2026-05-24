package net.focik.homeoffice.goahead.domain.cost;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.goahead.domain.cost.port.primary.AddCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.DeleteCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.UpdateCostUseCase;
import net.focik.homeoffice.goahead.domain.customer.ActiveStatus;
import net.focik.homeoffice.goahead.domain.invoice.KsefService;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.InvoiceKsefDto;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.goahead.domain.supplier.SupplierFacade;
import net.focik.homeoffice.utils.share.Module;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import pl.akmf.ksef.sdk.client.model.invoice.InvoiceQuerySubjectType;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class CostFacade implements AddCostUseCase, GetCostUseCase, UpdateCostUseCase, DeleteCostUseCase {

    private final CostService costService;
    private final KsefService ksefService;
    private final KsefCostMapper ksefCostMapper;
    private final FileRepository fileRepository;
    private final SupplierFacade supplierFacade;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Cost")
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
    public List<Cost> findBySellDateBetween(LocalDate from, LocalDate to) {
        return costService.findBySellDateBetween(from, to);
    }

    @Override
    public Page<Cost> findCostsPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, Integer idSupplier, LocalDate sellDate, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        return costService.findCostsPageableWithFilters(page, size, sortField, sortDirection, globalFilter, idSupplier, sellDate, dateComparisonType, amount, amountComparisonType, status);
    }

    @Override
    public KsefImportResult findKsefCosts(LocalDate fromDate, LocalDate toDate) {
        Map<InvoiceKsefDto, String> invoices = ksefService.findInvoices(fromDate, toDate, InvoiceQuerySubjectType.SUBJECT2);
        List<Cost> newCosts = new ArrayList<>();
        int duplicates = 0;

        for (Map.Entry<InvoiceKsefDto, String> entry : invoices.entrySet()) {
            InvoiceKsefDto invoice = entry.getKey();
            String metaData = entry.getValue();
            Cost cost = ksefCostMapper.toCost(invoice);
            cost.setInvoiceHash(ksefService.getFromJson(metaData, "invoiceHash"));
            cost.setKsefNumber(ksefService.getFromJson(metaData, "ksefNumber"));
            if(cost.getPaymentMethod() == null)
                cost.setPaymentMethod(PaymentMethod.CASH);

            resolveSupplier(cost);

            if (cost.getKsefNumber() != null && costService.existsByKsefNumber(cost.getKsefNumber())) {
                duplicates++;
                continue;
            }

            Cost addedCost = addCostWithCheck(cost);
            if (addedCost != null) {
                newCosts.add(cost);
            }
        }

        return new KsefImportResult(newCosts, invoices.size(), duplicates);
    }

    private Cost addCostWithCheck(Cost cost) {
        Cost addedCost = null;
        try {
            addedCost = costService.addCost(cost);
        } catch (Exception e) {
            log.error("Error saving cost from KSEF: {}", e.getMessage());
        }
        return addedCost;
    }

    private void resolveSupplier(Cost cost) {
        Supplier supplier = cost.getSupplier();
        if (supplier == null || supplier.getNip() == null) {
            return;
        }

        Optional<Supplier> existingSupplier = supplierFacade.findByNip(supplier.getNip());
        if (existingSupplier.isPresent()) {
            cost.setSupplier(existingSupplier.get());
        } else {
            supplier.setStatus(ActiveStatus.ACTIVE);
            Supplier savedSupplier = supplierFacade.addSupplier(supplier);
            cost.setSupplier(savedSupplier);
        }
    }

    @Override
    public String generateAndSendCostToS3(int idCost) {
        Cost cost = getCost(idCost);

        byte[] qrCode = ksefService.getQrCode(cost);

        String filePath = CostPdf.createPdf(cost, qrCode);
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        String s3Url = fileRepository.saveInBucket(file, Module.GO_AHEAD);
        file.delete();
        cost.setPdfUrl(s3Url);
        updateCost(cost);
        return s3Url;
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Cost")
    public Cost updateCost(Cost cost) {
        return costService.updateCost(cost);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Cost")
    public void updatePaymentStatus(Integer id, PaymentStatus paymentStatus) {
        costService.updatePaymentStatus(id, paymentStatus);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Cost")
    public void deleteCost(int id) {
        costService.deleteCost(id);
    }
}
