package net.focik.homeoffice.finance.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "finance_bank")
public class BankDbDto {
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
