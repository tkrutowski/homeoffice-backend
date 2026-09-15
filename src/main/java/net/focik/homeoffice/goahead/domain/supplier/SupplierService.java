package net.focik.homeoffice.goahead.domain.supplier;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.cost.port.secondary.CostRepository;
import net.focik.homeoffice.goahead.domain.customer.ActiveStatus;
import net.focik.homeoffice.goahead.domain.exception.CustomerAlreadyExistException;
import net.focik.homeoffice.goahead.domain.exception.CustomerNotFoundException;
import net.focik.homeoffice.goahead.domain.exception.SupplierCanNotBeDeletedException;
import net.focik.homeoffice.goahead.domain.supplier.port.secondary.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class SupplierService implements ISupplierService {

    private final SupplierRepository supplierRepository;
    // Port sekundarny modułu cost wstrzykniety bezposrednio (z pominieciem CostFacade), bo
    // CostFacade zalezy od SupplierFacade - wstrzykniecie CostFacade tutaj tworzyloby cykl.
    private final CostRepository costRepository;

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
        if (costRepository.existsBySupplier(id)) {
            log.warn("Supplier with ID {} cannot be deleted — associated costs found.", id);
            throw new SupplierCanNotBeDeletedException("koszty.");
        }
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

    public List<Supplier> findByAll(ActiveStatus activeStatus) {
        List<Supplier> supplierList = supplierRepository.findAll();

        if (activeStatus != null && activeStatus != ActiveStatus.ALL) {
            supplierList = supplierList.stream()
                    .filter(supplier -> activeStatus.equals(supplier.getStatus()))
                    .collect(Collectors.toList());
        }

        return supplierList;
    }

    public void updateSupplierStatus(Integer id, ActiveStatus status) {
        Supplier supplier = findById(id);
        supplier.changeCustomerStatus(status);

        supplierRepository.save(supplier);
    }

    @Override
    public Optional<Supplier> findByNip(String nip) {
        return supplierRepository.findByNip(nip);
    }
}
