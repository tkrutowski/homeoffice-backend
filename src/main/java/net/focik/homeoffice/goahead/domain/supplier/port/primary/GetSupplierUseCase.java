package net.focik.homeoffice.goahead.domain.supplier.port.primary;

import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;

import java.util.List;

public interface GetSupplierUseCase {
    Supplier findById(Integer id);

    List<Supplier> findByName(String name);

    List<Supplier> findByAll(CustomerStatus customerStatus);
}
