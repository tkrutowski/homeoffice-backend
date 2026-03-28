package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.goahead.domain.supplier.port.secondary.SupplierRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.SupplierDbDto;
import net.focik.homeoffice.goahead.infrastructure.mapper.JpaSupplierMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class SupplierRepositoryAdapter implements SupplierRepository {

    private final SupplierDtoRepository supplierDtoRepository;
    private final JpaSupplierMapper mapper;

    @Override
    public Supplier save(Supplier supplier) {
        SupplierDbDto supplierDbDto = mapper.toDto(supplier);
        if (supplierDbDto.getId() != null && supplierDbDto.getId() == 0){
            supplierDbDto.setId(null);
        }
        if (supplierDbDto.getAddress() != null && supplierDbDto.getAddress().getId() == 0){
            supplierDbDto.getAddress().setId(null);
        }
        SupplierDbDto save = supplierDtoRepository.save(supplierDbDto);
        return mapper.toDomain(save);
    }

    @Override
    public void delete(Integer id) {
        supplierDtoRepository.deleteById(id);
    }

    @Override
    public List<Supplier> findAll() {
        return supplierDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Supplier> findById(Integer id) {
        return supplierDtoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Supplier> findByNip(String nip) {
        return Optional.empty(); // To be implemented based on actual query if required
    }

    @Override
    public List<Supplier> findByName(String name) {
        return supplierDtoRepository.findAllByName(name).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Supplier> findAllByActive(CustomerStatus status) {
        return null; // To be implemented based on actual query if required
    }
}
