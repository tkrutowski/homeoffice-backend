package net.focik.homeoffice.goahead.domain.company;

import lombok.*;
import net.focik.homeoffice.addresses.domain.Address;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Company {
    private String name;
    private String fullName;
    private String nip;
    private String regon;
    private String phone;
    private String mail;
    private String www;
    private String bank;
    private String accountNo;
    private String info;
    private Address address;

    public String getNipWithoutDashes() {
        if (nip == null) {
            return "";
        }
        return nip.replace("-", "");
    }
}