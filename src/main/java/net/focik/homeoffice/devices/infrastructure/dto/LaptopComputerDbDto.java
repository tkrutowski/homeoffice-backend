package net.focik.homeoffice.devices.infrastructure.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.focik.homeoffice.audit.AuditableEntity;
import net.focik.homeoffice.utils.share.ActiveStatus;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@ToString
@Table(name = "devices_laptops")
public class LaptopComputerDbDto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_user")
    private Integer idUser;

    private String name;

    @Column(name = "cpu", length = 255)
    private String cpu;

    @Column(name = "gpu", length = 255)
    private String gpu;

    @Column(name = "ram", length = 255)
    private String ram;

    @Column(name = "storage", length = 255)
    private String storage;

    @Column(name = "display", length = 255)
    private String display;

    private String info;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ActiveStatus activeStatus;
}
