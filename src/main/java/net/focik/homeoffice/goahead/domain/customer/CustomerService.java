package net.focik.homeoffice.goahead.domain.customer;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.addresses.api.internal.AddressEndpoint;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.goahead.domain.customer.port.secondary.CustomerRepository;
import net.focik.homeoffice.goahead.domain.exception.CustomerAlreadyExistException;
import net.focik.homeoffice.goahead.domain.exception.CustomerNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class CustomerService implements ICustomerService {

    private final CustomerRepository customerRepository;
    private final AddressEndpoint addressEndpoint;

    @Transactional
    public Customer addCustomer(Customer customer) {
        validate(customer);
        Address savedAddress = addressEndpoint.addAddress(customer.getAddress());
        customer.setAddress(savedAddress);
        int id = customerRepository.save(customer).getId();
        return findById(id, true);
    }

    @Transactional
    public Customer updateCustomer(Customer customer) {
        Customer byId = findById(customer.getId(), false);
        customer.getAddress().setId(byId.getAddress().getId());

        Address updatedAddress = addressEndpoint.updateAddress(customer.getAddress());
        Customer saved = customerRepository.save(customer);
        saved.setAddress(updatedAddress);
        return saved;
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
        Customer byId = findById(id, true);
        addressEndpoint.deleteAddress(byId.getAddress().getId());
        customerRepository.delete(id);
    }

    public Customer findById(Integer id, Boolean isAddress) {
        Optional<Customer> byId = customerRepository.findById(id);
        if (byId.isEmpty()) {
            throw new CustomerNotFoundException("id", String.valueOf(id));
        }
        if (isAddress != null && isAddress) {
            byId.get().setAddress(addressEndpoint.getAddress(byId.get().getAddress().getId()));
        }
        return byId.get();
    }

    public List<Customer> findByName(String name) {
        List<Customer> byName = customerRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new CustomerNotFoundException("nazwie", name);
        }
        byName.forEach(customer -> customer.setAddress(addressEndpoint.getAddress(customer.getAddress().getId())));
        return byName;
    }

    public List<Customer> findByAll(CustomerStatus customerStatus, Boolean isGetAddress, CustomerType customerType) {
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

        if (isGetAddress != null && isGetAddress) {
            customerList
                    .forEach(customer -> customer.setAddress(addressEndpoint.getAddress(customer.getAddress().getId())));
        }

        return customerList;
    }

    public void updateCustomerStatus(Integer id, CustomerStatus status) {
        Customer customer = findById(id, false);
        customer.changeCustomerStatus(status);

        customerRepository.save(customer);
    }
}
