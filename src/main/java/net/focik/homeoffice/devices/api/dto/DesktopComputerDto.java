package net.focik.homeoffice.devices.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DesktopComputerDto extends ComputerDto {
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
}
