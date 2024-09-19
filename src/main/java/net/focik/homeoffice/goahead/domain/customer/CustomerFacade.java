package net.focik.homeoffice.goahead.domain.customer;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.customer.port.primary.AddCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.DeleteCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.GetCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.UpdateCustomerUseCase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class CustomerFacade implements AddCustomerUseCase, UpdateCustomerUseCase, GetCustomerUseCase, DeleteCustomerUseCase {

    private final ICustomerService customerService;

    public Customer addCustomer(Customer customer) {
         return customerService.addCustomer(customer);
    }

    public Customer updateCustomer(Customer customer) {
        return customerService.updateCustomer(customer);
    }

    @Override
    public void updateCustomerStatus(Integer id, CustomerStatus status) {
        customerService.updateCustomerStatus(id, status);
    }

    @Override
    public void deleteCustomer(Integer id) {
        customerService.deleteCustomer(id);
    }

    public Customer findById(Integer id, Boolean isAddress) {
        return customerService.findById(id, isAddress);
    }

    public List<Customer> findByName(String name) {
        return customerService.findByName(name);
    }

    public List<Customer> findByAll(CustomerStatus customerStatus, Boolean isGetAddress, CustomerType customerType) {
        return customerService.findByAll(customerStatus, isGetAddress, customerType);
    }
}
