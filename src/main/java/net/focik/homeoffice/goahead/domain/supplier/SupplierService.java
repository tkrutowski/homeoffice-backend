package net.focik.homeoffice.goahead.domain.supplier;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.exception.CustomerAlreadyExistException;
import net.focik.homeoffice.goahead.domain.exception.CustomerNotFoundException;
import net.focik.homeoffice.goahead.domain.supplier.port.secondary.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class SupplierService implements ISupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional
    public Supplier addSupplier(Supplier supplier) {
        validate(supplier);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier updateSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    private void validate(Supplier supplier) {
        Optional<Supplier> byNip = supplierRepository.findByNip(supplier.getNip());
        if (byNip.isPresent()) {
            throw new CustomerAlreadyExistException("Dostawca o NIP-ie " + supplier.getNip() + " już istnieje.");
        }
    }

    @Transactional
    public void deleteSupplier(Integer id) {
        supplierRepository.delete(id);
    }

    @Override
    public Supplier findById(Integer id) {
        Optional<Supplier> byId = supplierRepository.findById(id);
        if (byId.isEmpty()) {
            throw new CustomerNotFoundException("id", String.valueOf(id));
        }
        return byId.get();
    }

    public List<Supplier> findByName(String name) {
        List<Supplier> byName = supplierRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new CustomerNotFoundException("nazwie", name);
        }
        return byName;
    }

    public List<Supplier> findByAll(CustomerStatus customerStatus) {
        List<Supplier> supplierList = supplierRepository.findAll();

        if (customerStatus != null && customerStatus != CustomerStatus.ALL) {
            supplierList = supplierList.stream()
                    .filter(supplier -> customerStatus.equals(supplier.getCustomerStatus()))
                    .collect(Collectors.toList());
        }

        return supplierList;
    }

    public void updateSupplierStatus(Integer id, CustomerStatus status) {
        Supplier supplier = findById(id);
        supplier.changeCustomerStatus(status);

        supplierRepository.save(supplier);
    }
}
