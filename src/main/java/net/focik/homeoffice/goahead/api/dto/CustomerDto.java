package net.focik.homeoffice.goahead.api.dto;

import lombok.*;

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
    private CustomerTypeDto customerType;
    private CustomerStatusDto customerStatus;
    private String regon;
    private String otherInfo;
    private AddressDto address;


}