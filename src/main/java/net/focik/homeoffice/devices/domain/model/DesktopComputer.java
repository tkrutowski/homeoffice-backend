package net.focik.homeoffice.devices.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class DesktopComputer extends Computer {
    private Device processor;
    private Device motherboard;
    private List<Device> ram;
    private List<Device> disk;
    private Device power;
    private List<Device> cooling;
    private List<Device> display;
    private Device keyboard;
    private Device mouse;
    private Device computerCase;
    private Device soundCard;
    private List<Device> graphicCard;
    private List<Device> usb;
}
