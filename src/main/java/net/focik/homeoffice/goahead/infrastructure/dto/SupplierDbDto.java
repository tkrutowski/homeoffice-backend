package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;

import jakarta.persistence.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@ToString
@Table(name = "goahead_supplier")
public class SupplierDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "idAddress")
    private AddressDbDto address;
    private String name;
    private String nip;
    private String phone;
    private String mail;
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
    private String accountNumber;

}
