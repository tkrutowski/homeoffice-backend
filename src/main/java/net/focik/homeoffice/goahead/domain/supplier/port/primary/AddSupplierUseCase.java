package net.focik.homeoffice.goahead.domain.supplier.port.primary;

import net.focik.homeoffice.goahead.domain.supplier.Supplier;

public interface AddSupplierUseCase {
    Supplier addSupplier(Supplier supplier);
}
