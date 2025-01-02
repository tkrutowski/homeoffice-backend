package net.focik.homeoffice.devices.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.devices.domain.model.ComputerType;
import net.focik.homeoffice.utils.share.ActiveStatus;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "devices_computer")
public
class
ComputerDbDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "id_user")
    private Integer idUser;
    private String name;
    private Integer processor;
    private Integer motherboard;
    private String ram;
    private String disk;
    private Integer power;
    private String cooling;
    private String display;
    private Integer keyboard;
    private Integer mouse;
    private Integer computerCase;
    private Integer soundCard;
    private String graphicCard;
    private String usb;
    private String info;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ActiveStatus activeStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ComputerType computerType;
}