package net.focik.homeoffice.devices.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.utils.share.ActiveStatus;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "devices_computer")
public
class ComputerDbDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private AppUser user;
    private String name;
    private String info;
    @Enumerated(EnumType.STRING)
    private ActiveStatus activeStatus;
}