package net.focik.homeoffice.goahead.domain.customer.port.primary;

import net.focik.homeoffice.goahead.domain.customer.Customer;

public interface AddCustomerUseCase {
    Customer addCustomer(Customer customer);
}
