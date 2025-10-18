package net.focik.homeoffice.devices.infrastructure.mapper;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.infrastructure.dto.ComputerDbDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JpaComputerMapper {

    public ComputerDbDto toDto(Computer computer) {
        return ComputerDbDto.builder()
                .id(computer.getId())
                .idUser(computer.getIdUser())
                .disk(computer.getDisk() != null ? getIdFromList(computer.getDisk()) : null)
                .computerCase(computer.getComputerCase() != null ? computer.getComputerCase().getId() : null)
                .mouse(computer.getMouse() != null ? computer.getMouse().getId() : null)
                .power(computer.getPower() != null ? computer.getPower().getId() : null)
                .ram(computer.getRam() != null ? getIdFromList(computer.getRam()) : null)
                .usb(computer.getUsb() != null ? getIdFromList(computer.getUsb()) : null)
                .cooling(computer.getCooling() != null ? getIdFromList(computer.getCooling()) : null)
                .computerType(computer.getComputerType() != null ? computer.getComputerType() : null)
                .display(computer.getDisplay() != null ? getIdFromList(computer.getDisplay()) : null)
                .keyboard(computer.getKeyboard() != null ? computer.getKeyboard().getId() : null)
                .graphicCard(computer.getGraphicCard() != null ? getIdFromList(computer.getGraphicCard()) : null)
                .processor(computer.getProcessor() != null ? computer.getProcessor().getId() : null)
                .motherboard(computer.getMotherboard() != null ? computer.getMotherboard().getId() : null)
                .activeStatus(computer.getStatus() != null ? computer.getStatus() : null)
                .info(computer.getInfo() != null ? computer.getInfo() : null)
                .soundCard(computer.getSoundCard() != null ? computer.getSoundCard().getId() : null)
                .name(computer.getName() != null ? computer.getName() : null)
                .build();
    }

    public Computer toDomain(ComputerDbDto dto) {
        
        return Computer.builder()
                .id(dto.getId())
                .idUser(dto.getIdUser())
                .disk(Optional.ofNullable(dto.getDisk()).map(this::getDeviceFromList).orElse(List.of()))
                .computerCase(Optional.ofNullable(dto.getComputerCase()).map(id -> Device.builder().id(id).build()).orElse(null))
                .mouse(Optional.ofNullable(dto.getMouse()).map(id -> Device.builder().id(id).build()).orElse(null))
                .power(Optional.ofNullable(dto.getPower()).map(id -> Device.builder().id(id).build()).orElse(null))
                .ram(Optional.ofNullable(dto.getRam()).map(this::getDeviceFromList).orElse(List.of()))
                .usb(Optional.ofNullable(dto.getUsb()).map(this::getDeviceFromList).orElse(List.of()))
                .cooling(Optional.ofNullable(dto.getCooling()).map(this::getDeviceFromList).orElse(List.of()))
                .computerType(dto.getComputerType())
                .display(Optional.ofNullable(dto.getDisplay()).map(this::getDeviceFromList).orElse(List.of()))
                .keyboard(Optional.ofNullable(dto.getKeyboard()).map(id -> Device.builder().id(id).build()).orElse(null))
                .graphicCard(Optional.ofNullable(dto.getGraphicCard()).map(this::getDeviceFromList).orElse(List.of()))
                .processor(Optional.ofNullable(dto.getProcessor()).map(id -> Device.builder().id(id).build()).orElse(null))
                .motherboard(Optional.ofNullable(dto.getMotherboard()).map(id -> Device.builder().id(id).build()).orElse(null))
                .status(dto.getActiveStatus())
                .info(dto.getInfo())
                .soundCard(Optional.ofNullable(dto.getSoundCard()).map(id -> Device.builder().id(id).build()).orElse(null))
                .name(dto.getName())
                .build();
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