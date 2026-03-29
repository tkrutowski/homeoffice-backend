package net.focik.homeoffice.goahead.api.dto;

import lombok.*;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class SupplierDto {

    private int id;
    private String name;
    private String nip;
    private String phone;
    private String mail;
    private CustomerStatus customerStatus;
    private String otherInfo;
    private AddressDto address;
    private String accountNumber;

}