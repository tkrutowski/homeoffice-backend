package net.focik.homeoffice.devices.infrastructure.mapper;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.DesktopComputer;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.infrastructure.dto.DesktopComputerDbDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JpaDesktopComputerMapper {

    public DesktopComputerDbDto toDto(DesktopComputer computer) {
        DesktopComputerDbDto dto = new DesktopComputerDbDto();
        dto.setId(computer.getId());
        dto.setIdUser(computer.getIdUser());
        dto.setName(computer.getName() != null ? computer.getName() : null);
        dto.setProcessor(computer.getProcessor() != null ? computer.getProcessor().getId() : null);
        dto.setMotherboard(computer.getMotherboard() != null ? computer.getMotherboard().getId() : null);
        dto.setRam(computer.getRam() != null ? getIdFromList(computer.getRam()) : null);
        dto.setDisk(computer.getDisk() != null ? getIdFromList(computer.getDisk()) : null);
        dto.setPower(computer.getPower() != null ? computer.getPower().getId() : null);
        dto.setCooling(computer.getCooling() != null ? getIdFromList(computer.getCooling()) : null);
        dto.setDisplay(computer.getDisplay() != null ? getIdFromList(computer.getDisplay()) : null);
        dto.setKeyboard(computer.getKeyboard() != null ? computer.getKeyboard().getId() : null);
        dto.setMouse(computer.getMouse() != null ? computer.getMouse().getId() : null);
        dto.setComputerCase(computer.getComputerCase() != null ? computer.getComputerCase().getId() : null);
        dto.setSoundCard(computer.getSoundCard() != null ? computer.getSoundCard().getId() : null);
        dto.setGraphicCard(computer.getGraphicCard() != null ? getIdFromList(computer.getGraphicCard()) : null);
        dto.setUsb(computer.getUsb() != null ? getIdFromList(computer.getUsb()) : null);
        dto.setActiveStatus(computer.getStatus() != null ? computer.getStatus() : null);
        dto.setInfo(computer.getInfo() != null ? computer.getInfo() : null);
        return dto;
    }

    public DesktopComputer toDomain(DesktopComputerDbDto dto) {
        DesktopComputer computer = new DesktopComputer();
        computer.setId(dto.getId());
        computer.setIdUser(dto.getIdUser());
        computer.setName(dto.getName());
        computer.setProcessor(Optional.ofNullable(dto.getProcessor()).map(id -> Device.builder().id(id).build()).orElse(null));
        computer.setMotherboard(Optional.ofNullable(dto.getMotherboard()).map(id -> Device.builder().id(id).build()).orElse(null));
        computer.setRam(Optional.ofNullable(dto.getRam()).map(this::getDeviceFromList).orElse(List.of()));
        computer.setDisk(Optional.ofNullable(dto.getDisk()).map(this::getDeviceFromList).orElse(List.of()));
        computer.setPower(Optional.ofNullable(dto.getPower()).map(id -> Device.builder().id(id).build()).orElse(null));
        computer.setCooling(Optional.ofNullable(dto.getCooling()).map(this::getDeviceFromList).orElse(List.of()));
        computer.setDisplay(Optional.ofNullable(dto.getDisplay()).map(this::getDeviceFromList).orElse(List.of()));
        computer.setKeyboard(Optional.ofNullable(dto.getKeyboard()).map(id -> Device.builder().id(id).build()).orElse(null));
        computer.setMouse(Optional.ofNullable(dto.getMouse()).map(id -> Device.builder().id(id).build()).orElse(null));
        computer.setComputerCase(Optional.ofNullable(dto.getComputerCase()).map(id -> Device.builder().id(id).build()).orElse(null));
        computer.setSoundCard(Optional.ofNullable(dto.getSoundCard()).map(id -> Device.builder().id(id).build()).orElse(null));
        computer.setGraphicCard(Optional.ofNullable(dto.getGraphicCard()).map(this::getDeviceFromList).orElse(List.of()));
        computer.setUsb(Optional.ofNullable(dto.getUsb()).map(this::getDeviceFromList).orElse(List.of()));
        computer.setStatus(dto.getActiveStatus());
        computer.setInfo(dto.getInfo());
        return computer;
    }

    private String getIdFromList(List<Device> list) {
        return list.stream().map(Device::getId)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private List<Device> getDeviceFromList(String input) {
        List<Device> deviceList = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            log.warn("Input string is null or empty.");
            return deviceList;
        }
        log.info("Processing input string: {}", input);
        String[] ids = input.split(",");
        log.debug("Splited input: {}", (Object) ids);
        for (String id : ids) {
            log.debug("Processing id: {}", id);
            deviceList.add(Device.builder().id(Integer.parseInt(id)).build());
        }
        return deviceList;
    }
}
