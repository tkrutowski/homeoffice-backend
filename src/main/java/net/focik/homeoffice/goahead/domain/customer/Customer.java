package net.focik.homeoffice.goahead.domain.customer;

import lombok.*;
import net.focik.homeoffice.addresses.domain.Address;
import org.apache.commons.lang3.StringUtils;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Customer{

    private int id;
    private String name;
    private String firstName;
    private String nip;
    private String phone;
    private String mail;
    private CustomerType customerType;
    private String otherInfo;
    private CustomerStatus customerStatus;
    private String regon;
    private Address address;

    public void  updateAddress(String city, String street, String zip) {
        if (StringUtils.isNotEmpty(city)){
            this.address.setCity(city);
        }
        if (StringUtils.isNotEmpty(street)){
            this.address.setStreet(street);
        }
        if (StringUtils.isNotEmpty(zip)){
            this.address.setZip(zip);
        }
    }

    public void setAddress(String city, String street, String zip) {
        this.address = new Address(0, city, street, zip);
    }

    public void changeCustomerStatus(CustomerStatus newCustomerStatus) {
        this.customerStatus = newCustomerStatus;
    }

}