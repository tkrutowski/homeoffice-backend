package net.focik.homeoffice.devices.infrastructure.mapper;

import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.infrastructure.dto.ComputerDbDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JpaComputerMapper {

    public ComputerDbDto toDto(Computer computer) {
        return ComputerDbDto.builder()
                .id(computer.getId())
                .idUser(computer.getIdUser())
                .disk(getIdFromList(computer.getDisk()))
                .computerCase(computer.getComputerCase().getId())
                .mouse(computer.getMouse().getId())
                .power(computer.getPower().getId())
                .ram(getIdFromList(computer.getRam()))
                .usb(getIdFromList(computer.getUsb()))
                .cooling(getIdFromList(computer.getCooling()))
                .computerType(computer.getComputerType())
                .display(getIdFromList(computer.getDisplay()))
                .keyboard(computer.getKeyboard().getId())
                .graphicCard(getIdFromList(computer.getGraphicCard()))
                .processor(computer.getProcessor().getId())
                .motherboard(computer.getMotherboard().getId())
                .activeStatus(computer.getStatus())
                .info(computer.getInfo())
                .soundCard(computer.getSoundCard().getId())
                .name(computer.getName())
                .build();
    }

    private String getIdFromList(List<Device> list) {
        return list.stream().map(Device::getId)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }
}