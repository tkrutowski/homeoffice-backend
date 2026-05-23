package net.focik.homeoffice.goahead.domain.customer.port.primary;

import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.ActiveStatus;

public interface UpdateCustomerUseCase {
    Customer updateCustomer(Customer customer);

    void updateCustomerStatus(Integer id, ActiveStatus valueOf);
}
