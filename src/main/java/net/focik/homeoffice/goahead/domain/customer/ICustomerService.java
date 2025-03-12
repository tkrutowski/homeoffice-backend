package net.focik.homeoffice.goahead.domain.customer;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ICustomerService {

    Customer addCustomer(Customer customer);

    Customer updateCustomer(Customer customer);

    void deleteCustomer(Integer id);

    Customer findById(Integer id);

    List<Customer> findByName(String name);

    List<Customer> findByAll(CustomerStatus customerStatus, CustomerType customerType);

    void updateCustomerStatus(Integer id, CustomerStatus status);
}
