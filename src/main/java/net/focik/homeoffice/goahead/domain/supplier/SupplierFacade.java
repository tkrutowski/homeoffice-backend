package net.focik.homeoffice.goahead.domain.supplier;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
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
    public Supplier addSupplier(Supplier supplier) {
         return supplierService.addSupplier(supplier);
    }

    @Override
    public Supplier updateSupplier(Supplier supplier) {
        return supplierService.updateSupplier(supplier);
    }

    @Override
    public void updateSupplierStatus(Integer id, CustomerStatus status) {
        supplierService.updateSupplierStatus(id, status);
    }

    @Override
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
    public List<Supplier> findByAll(CustomerStatus customerStatus) {
        return supplierService.findByAll(customerStatus);
    }
}
