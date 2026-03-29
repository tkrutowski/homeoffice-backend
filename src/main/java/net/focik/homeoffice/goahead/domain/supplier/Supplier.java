package net.focik.homeoffice.goahead.domain.supplier;

import lombok.*;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import org.apache.commons.lang3.StringUtils;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Supplier {

    private int id;
    private String name;
    private String nip;
    private String phone;
    private String mail;
    private String otherInfo;
    private CustomerStatus customerStatus;
    private Address address;
    private String accountNumber;

    public void updateAddress(String city, String street, String zip) {
        if (StringUtils.isNotEmpty(city)) {
            this.address.setCity(city);
        }
        if (StringUtils.isNotEmpty(street)) {
            this.address.setStreet(street);
        }
        if (StringUtils.isNotEmpty(zip)) {
            this.address.setZip(zip);
        }
    }

    public void setAddress(String city, String street, String zip) {
        this.address = new Address(null, city, street, zip);
    }

    public void changeCustomerStatus(CustomerStatus newCustomerStatus) {
        this.customerStatus = newCustomerStatus;
    }

    public String getNipWithoutDashes() {
        if (nip == null) {
            return "";
        }
        return nip.replace("-", "");
    }
}
