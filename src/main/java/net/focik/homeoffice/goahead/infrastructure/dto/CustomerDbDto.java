package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;

import jakarta.persistence.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "goahead_customer")
public class CustomerDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "idAddress")
    private AddressDbDto address;
    private String name;
    private String firstName;
    private String nip;
    private String regon;
    private String phone;
    private String mail;
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
    @Enumerated(EnumType.STRING)
    private CustomerType type;

}
