package net.focik.homeoffice.devices.domain.model;


import lombok.*;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Computer {
        private Integer id;
        private Integer idUser;
        private String  name;
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
        private ActiveStatus status;
        private ComputerType computerType;
        private String info;
}
