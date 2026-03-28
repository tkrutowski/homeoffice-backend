package net.focik.homeoffice.goahead.domain.supplier.port.primary;

import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;

public interface UpdateSupplierUseCase {
    Supplier updateSupplier(Supplier supplier);
    void updateSupplierStatus(Integer id, CustomerStatus status);
}
