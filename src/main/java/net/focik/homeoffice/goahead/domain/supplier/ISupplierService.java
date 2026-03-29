package net.focik.homeoffice.goahead.domain.supplier;

import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ISupplierService {

    Supplier addSupplier(Supplier supplier);

    Supplier updateSupplier(Supplier supplier);

    void deleteSupplier(Integer id);

    Supplier findById(Integer id);

    List<Supplier> findByName(String name);

    List<Supplier> findByAll(CustomerStatus customerStatus);

    void updateSupplierStatus(Integer id, CustomerStatus status);
}
