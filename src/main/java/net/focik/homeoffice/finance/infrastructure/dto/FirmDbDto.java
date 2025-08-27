package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.*;

import jakarta.persistence.*;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "finance_firm")
public class FirmDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "idAddress")
    private AddressDbDto address;
    private String name;
    private String phone;
    private String phone2;
    private String fax;
    private String mail;
    private String www;
    private String otherInfo;
}