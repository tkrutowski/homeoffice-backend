package net.focik.homeoffice.goahead.domain.supplier;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.goahead.domain.customer.ActiveStatus;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.AddSupplierUseCase;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.DeleteSupplierUseCase;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.GetSupplierUseCase;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.UpdateSupplierUseCase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class SupplierFacade implements AddSupplierUseCase, UpdateSupplierUseCase, GetSupplierUseCase, DeleteSupplierUseCase {

    private final ISupplierService supplierService;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Supplier")
    public Supplier addSupplier(Supplier supplier) {
         return supplierService.addSupplier(supplier);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Supplier")
    public Supplier updateSupplier(Supplier supplier) {
        return supplierService.updateSupplier(supplier);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Supplier")
    public void updateSupplierStatus(Integer id, ActiveStatus status) {
        supplierService.updateSupplierStatus(id, status);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Supplier")
    public void deleteSupplier(Integer id) {
        supplierService.deleteSupplier(id);
    }

    @Override
    public Supplier findById(Integer id) {
        return supplierService.findById(id);
    }

    @Override
    public List<Supplier> findByName(String name) {
        return supplierService.findByName(name);
    }

    @Override
    public List<Supplier> findByAll(ActiveStatus status) {
        return supplierService.findByAll(status);
    }

    public java.util.Optional<Supplier> findByNip(String nip) {
        return supplierService.findByNip(nip);
    }
}
