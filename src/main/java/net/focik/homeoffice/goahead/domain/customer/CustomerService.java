package net.focik.homeoffice.goahead.domain.customer;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.customer.port.secondary.CustomerRepository;
import net.focik.homeoffice.goahead.domain.exception.CustomerAlreadyExistException;
import net.focik.homeoffice.goahead.domain.exception.CustomerNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class CustomerService implements ICustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer addCustomer(Customer customer) {
        validate(customer);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    private void validate(Customer customer) {
        if (customer.getCustomerType() == CustomerType.COMPANY) {
            Optional<Customer> byNip = customerRepository.findByNip(customer.getNip());
            if (byNip.isPresent()) {
                throw new CustomerAlreadyExistException("Klient o NIP-ie " + customer.getNip() + "już istnieje.");
            }
        }
    }

    @Transactional
    public void deleteCustomer(Integer id) {
        customerRepository.delete(id);
    }

    @Override
    public Customer findById(Integer id) {
        Optional<Customer> byId = customerRepository.findById(id);
        if (byId.isEmpty()) {
            throw new CustomerNotFoundException("id", String.valueOf(id));
        }
        return byId.get();
    }

    public List<Customer> findByName(String name) {
        List<Customer> byName = customerRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new CustomerNotFoundException("nazwie", name);
        }
        return byName;
    }

    public List<Customer> findByAll(CustomerStatus customerStatus, CustomerType customerType) {
        List<Customer> customerList = customerRepository.findAll();

        if (customerStatus != null && customerStatus != CustomerStatus.ALL) {
            customerList = customerList.stream()
                    .filter(customer -> customerStatus.equals(customer.getCustomerStatus()))
                    .collect(Collectors.toList());
        }

        if (customerType != null) {
            customerList = customerList.stream()
                    .filter(customer -> customerType.equals(customer.getCustomerType()))
                    .collect(Collectors.toList());
        }

        return customerList;
    }

    public void updateCustomerStatus(Integer id, CustomerStatus status) {
        Customer customer = findById(id);
        customer.changeCustomerStatus(status);

        customerRepository.save(customer);
    }
}
