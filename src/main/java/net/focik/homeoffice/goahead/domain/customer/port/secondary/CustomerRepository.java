package net.focik.homeoffice.goahead.domain.customer.port.secondary;

import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface CustomerRepository {

    Customer save(Customer customer);

    void delete(Integer id);

    List<Customer> findAll();

    Optional<Customer> findById(Integer id);

    Optional<Customer> findByNip(String nip);

    List<Customer> findByName(String name);

    List<Customer> findAllByActive(CustomerStatus status);
}
