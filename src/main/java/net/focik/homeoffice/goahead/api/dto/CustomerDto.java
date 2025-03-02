package net.focik.homeoffice.goahead.api.dto;

import lombok.*;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CustomerDto {

    private int id;
    private String name;
    private String firstName;
    private String nip;
    private String phone;
    private String mail;
    private CustomerType customerType;
    private CustomerStatus customerStatus;
    private String regon;
    private String otherInfo;
    private AddressDto address;


}