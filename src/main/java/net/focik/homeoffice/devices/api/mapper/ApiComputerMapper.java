package net.focik.homeoffice.devices.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.api.dto.ComputerDto;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.Device;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiComputerMapper {

    public Computer toDomain(ComputerDto dto) {
        return Computer.builder()
                .id(dto.getId())
                .idUser(dto.getIdUser())
                .disk(getDevices(dto.getDisk()))
                .computerCase(getDevice(dto.getComputerCase()))
                .mouse(getDevice(dto.getMouse()))
                .power(getDevice(dto.getPower()))
                .ram(getDevices(dto.getRam()))
                .usb(getDevices(dto.getUsb()))
                .cooling(getDevices(dto.getCooling()))
                .computerType(dto.getComputerType())
                .display(getDevices(dto.getDisplay()))
                .keyboard(getDevice(dto.getKeyboard()))
                .graphicCard(getDevices(dto.getGraphicCard()))
                .processor(getDevice(dto.getProcessor()))
                .motherboard(getDevice(dto.getMotherboard()))
                .status(dto.getActiveStatus())
                .info(dto.getInfo())
                .soundCard(getDevice(dto.getSoundCard()))
                .name(dto.getName())
                .build();
    }

    private Device getDevice(Integer id) {
        Device device = null;
        if (id != null) {
            device = new Device();
            device.setId(id);
        }
        return device;
    }

    private List<Device> getDevices(List<Integer> ids) {
        List<Device> devices = new ArrayList<>();
        ids.forEach(id -> devices.add(getDevice(id)));
        return devices;
    }

    public ComputerDto toDto(Computer computer) {
        return ComputerDto.builder()
                .id(computer.getId())
                .idUser(computer.getIdUser())
                .disk(getIdFromList(computer.getDisk()))
                .computerCase(getIdFromDevice(computer.getComputerCase()))
                .mouse(getIdFromDevice(computer.getMouse()))
                .power(getIdFromDevice(computer.getPower()))
                .ram(getIdFromList(computer.getRam()))
                .usb(getIdFromList(computer.getUsb()))
                .cooling(getIdFromList(computer.getCooling()))
                .computerType(computer.getComputerType())
                .display(getIdFromList(computer.getDisplay()))
                .keyboard(getIdFromDevice(computer.getKeyboard()))
                .graphicCard(getIdFromList(computer.getGraphicCard()))
                .processor(getIdFromDevice(computer.getProcessor()))
                .motherboard(getIdFromDevice(computer.getMotherboard()))
                .activeStatus(computer.getStatus())
                .info(computer.getInfo())
                .soundCard(getIdFromDevice(computer.getSoundCard()))
                .name(computer.getName())
                .build();
    }

    private Integer getIdFromDevice(Device device) {
        if (device == null) {
            return -1;
        }
        return device.getId();
    }

    private List<Integer> getIdFromList(List<Device> list) {
        return list.stream().map(Device::getId)
                .toList();
    }
}