package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.customer.port.secondary.CustomerRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;
import net.focik.homeoffice.goahead.infrastructure.mapper.JpaCustomerMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerDtoRepository customerDtoRepository;
    private final JpaCustomerMapper mapper;

    @Override
    public Customer save(Customer customer) {
        CustomerDbDto customerDbDto = mapper.toDto(customer);
        if (customerDbDto.getId() != null && customerDbDto.getId() == 0){
            customerDbDto.setId(null);
        }
        CustomerDbDto save = customerDtoRepository.save(customerDbDto);
        return mapper.toDomain(save);
    }

    @Override
    public void delete(Integer id) {
        customerDtoRepository.deleteById(id);
    }


    @Override
    public List<Customer> findAll() {
        return customerDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        return customerDtoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByNip(String nip) {
        return Optional.empty();
    }

    @Override
    public List<Customer> findByName(String name) {
        return null;
    }

    @Override
    public List<Customer> findAllByActive(CustomerStatus status) {
        return null;
    }
}
