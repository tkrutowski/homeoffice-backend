package net.focik.homeoffice.goahead.infrastructure.inMemory;

import lombok.extern.java.Log;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.customer.port.secondary.CustomerRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;
import net.focik.homeoffice.goahead.infrastructure.mapper.JpaCustomerMapper;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Profile({"test"})
@Log
public class InMemoryCustomerRepositoryAdapter implements CustomerRepository {

    //    JpaAdvanceMapper jpaMapper = new JpaAdvanceMapper();
    private final JpaCustomerMapper mapper = new JpaCustomerMapper();

    @Override
    public Customer save(Customer customer) {
        if (customer == null)
            throw new NullPointerException("Customer cannot be null");

        if (customer.getId() <=0) {
            return add(customer);
        } else {
            return update(customer);
        }

    }

    @Override
    public void delete(Integer id) {
        DataBaseCustomer.getCustomerHashMap().remove(id);
    }

    @Override
    public List<Customer> findAll() {
        return DataBaseCustomer.getCustomerHashMap()
                .values()
                .stream()
                .map(customerDbDto -> mapper.toDomain(customerDbDto))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        return Optional.ofNullable(DataBaseCustomer.getCustomerHashMap().get(id))
                .map(dbDto -> mapper.toDomain(dbDto));
    }


    @Override
    public Optional<Customer> findByNip(String nip) {
        return DataBaseCustomer.getCustomerHashMap()
                .values()
                .stream()
                .filter(customerDbDto -> nip.equals(customerDbDto.getNip()))
                .map(customerDbDto -> mapper.toDomain(customerDbDto))
                .findFirst();
    }

    @Override
    public List<Customer> findByName(String name) {
        return DataBaseCustomer.getCustomerHashMap()
                .values()
                .stream()
                .filter(customerDbDto -> customerDbDto.getName().equals(name))
                .map(customerDbDto -> mapper.toDomain(customerDbDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findAllByActive(CustomerStatus status) {
        return DataBaseCustomer.getCustomerHashMap()
                .values()
                .stream()
                .filter(customerDbDto -> customerDbDto.getStatus().equals(status))
                .map(customerDbDto -> mapper.toDomain(customerDbDto))
                .collect(Collectors.toList());
    }

    private Customer add(Customer customer) {
        CustomerDbDto customerDbDto = mapper.toDto(customer);
        log.info("Try add into inMemoryDb customer: " + customerDbDto.toString());
        if (customerDbDto == null)
            throw new NullPointerException("Advance cannot be null");
        Integer id = DataBaseCustomer.getCustomerHashMap()
                .keySet()
                .stream()
                .reduce(Integer::max)
                .orElse(Integer.valueOf("0")) + 1;

        if (customerDbDto.getId() == 0) {
            customerDbDto.setId(id);
        }
        // employee.setId(id);
        DataBaseCustomer.getCustomerHashMap().put(id, customerDbDto);
        log.info("Succssec id = " + id);
        CustomerDbDto dbDto = DataBaseCustomer.getCustomerHashMap().get(id);
        return mapper.toDomain(dbDto);
    }

    private Customer update(Customer customer) {
        CustomerDbDto customerDbDto = mapper.toDto(customer);
        log.info("Try update into inMemoryDb customer: " + customerDbDto.toString());
        if (customerDbDto == null)
            throw new NullPointerException("Advance cannot be null");
        DataBaseCustomer.getCustomerHashMap().put(customer.getId(), customerDbDto);
        log.info("Succssec id = " + customer.getId());
        CustomerDbDto dbDto = DataBaseCustomer.getCustomerHashMap().get(customer.getId());
        return mapper.toDomain(dbDto);
    }
}
