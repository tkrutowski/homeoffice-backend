package net.focik.homeoffice.finance.api.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class FirmDto {

    private int id;
    private String name;
    private String phone;
    private String phone2;
    private String mail;
    private String fax;
    private String otherInfo;
    private AddressDto address;
    private String www;
}