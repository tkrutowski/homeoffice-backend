package net.focik.homeoffice.goahead.domain.customer.port.primary;

import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.ActiveStatus;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;

import java.util.List;

public interface GetCustomerUseCase {
    Customer findById(Integer id);
    List<Customer> findByName(String name);
    List<Customer> findByAll(ActiveStatus activeStatus, CustomerType customerType);
}
