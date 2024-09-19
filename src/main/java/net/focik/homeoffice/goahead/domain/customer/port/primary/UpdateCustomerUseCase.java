package net.focik.homeoffice.goahead.domain.customer.port.primary;

import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;

public interface UpdateCustomerUseCase {
    Customer updateCustomer(Customer customer);

    void updateCustomerStatus(Integer id, CustomerStatus valueOf);
}
