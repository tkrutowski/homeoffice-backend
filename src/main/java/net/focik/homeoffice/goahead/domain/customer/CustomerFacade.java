package net.focik.homeoffice.goahead.domain.customer;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
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

    @AuditLog(action = AuditAction.CREATE, entityType = "Customer")
    public Customer addCustomer(Customer customer) {
         return customerService.addCustomer(customer);
    }

    @AuditLog(action = AuditAction.UPDATE, entityType = "Customer")
    public Customer updateCustomer(Customer customer) {
        return customerService.updateCustomer(customer);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Customer")
    public void updateCustomerStatus(Integer id, ActiveStatus status) {
        customerService.updateCustomerStatus(id, status);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Customer")
    public void deleteCustomer(Integer id) {
        customerService.deleteCustomer(id);
    }

    public Customer findById(Integer id) {
        return customerService.findById(id);
    }

    public List<Customer> findByName(String name) {
        return customerService.findByName(name);
    }

    public List<Customer> findByAll(ActiveStatus activeStatus, CustomerType customerType) {
        return customerService.findByAll(activeStatus, customerType);
    }
}
