package net.focik.homeoffice.goahead.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "goahead_company")
public class CompanyDbDto {
    @Id
    private String name;
    private String fullName;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "idAddress")
    private AddressDbDto address;
    private String nip;
    private String regon;
    private String phone;
    private String mail;
    private String www;
    private String bank;
    private String accountNo;
    private String info;

}