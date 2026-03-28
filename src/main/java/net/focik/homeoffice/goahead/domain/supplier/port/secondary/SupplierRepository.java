package net.focik.homeoffice.goahead.domain.supplier.port.secondary;

import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface SupplierRepository {

    Supplier save(Supplier supplier);

    void delete(Integer id);

    List<Supplier> findAll();

    Optional<Supplier> findById(Integer id);

    Optional<Supplier> findByNip(String nip);

    List<Supplier> findByName(String name);

    List<Supplier> findAllByActive(CustomerStatus status);
}
