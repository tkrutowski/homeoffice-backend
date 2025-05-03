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
    private Integer processor;
    private Integer motherboard;
    private List<Integer> ram;
    private List<Integer> disk;
    private Integer power;
    private List<Integer> cooling;
    private List<Integer> display;
    private Integer keyboard;
    private Integer mouse;
    private Integer computerCase;
    private Integer soundCard;
    private List<Integer> graphicCard;
    private List<Integer> usb;
    private String info;
    private ActiveStatus activeStatus;
    private ComputerType computerType;
}