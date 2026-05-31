package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;
import net.focik.homeoffice.audit.AuditableEntity;
import net.focik.homeoffice.goahead.domain.customer.ActiveStatus;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;

import jakarta.persistence.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@ToString
@Table(name = "goahead_customer")
public class CustomerDbDto extends AuditableEntity {
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
    @Column(name = "status")
    private ActiveStatus activeStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private CustomerType customerType;

}
