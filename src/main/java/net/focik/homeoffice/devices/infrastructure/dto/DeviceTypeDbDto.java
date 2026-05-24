package net.focik.homeoffice.devices.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.audit.AuditableEntity;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@ToString
@Table(name = "devices_type")
public
class DeviceTypeDbDto extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    private String name;
}