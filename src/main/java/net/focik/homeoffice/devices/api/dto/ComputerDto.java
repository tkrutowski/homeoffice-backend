package net.focik.homeoffice.devices.api.dto;

import lombok.*;
import net.focik.homeoffice.devices.domain.model.ComputerType;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class ComputerDto {
    private Integer id;
    private Integer idUser;
    private String name;
    private DeviceDto processor;
    private DeviceDto motherboard;
    private List<DeviceDto> ram;
    private List<DeviceDto> disk;
    private DeviceDto power;
    private List<DeviceDto> cooling;
    private List<DeviceDto> display;
    private DeviceDto keyboard;
    private DeviceDto mouse;
    private DeviceDto computerCase;
    private DeviceDto soundCard;
    private List<DeviceDto> graphicCard;
    private List<DeviceDto> usb;
    private String info;
    private ActiveStatus activeStatus;
    private ComputerType computerType;
}