package net.focik.homeoffice.devices.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.api.dto.DesktopComputerDto;
import net.focik.homeoffice.devices.domain.model.DesktopComputer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiDesktopComputerMapper {
    private final ApiDeviceMapper apiDeviceMapper;

    public DesktopComputer toDomain(DesktopComputerDto dto) {
        DesktopComputer computer = new DesktopComputer();
        computer.setId(dto.getId());
        computer.setIdUser(dto.getIdUser());
        computer.setName(dto.getName());
        computer.setStatus(dto.getActiveStatus());
        computer.setInfo(dto.getInfo());
        computer.setProcessor(apiDeviceMapper.toDomain(dto.getProcessor()));
        computer.setMotherboard(apiDeviceMapper.toDomain(dto.getMotherboard()));
        computer.setRam(apiDeviceMapper.toDomain(dto.getRam()));
        computer.setDisk(apiDeviceMapper.toDomain(dto.getDisk()));
        computer.setPower(apiDeviceMapper.toDomain(dto.getPower()));
        computer.setCooling(apiDeviceMapper.toDomain(dto.getCooling()));
        computer.setDisplay(apiDeviceMapper.toDomain(dto.getDisplay()));
        computer.setKeyboard(apiDeviceMapper.toDomain(dto.getKeyboard()));
        computer.setMouse(apiDeviceMapper.toDomain(dto.getMouse()));
        computer.setComputerCase(apiDeviceMapper.toDomain(dto.getComputerCase()));
        computer.setSoundCard(apiDeviceMapper.toDomain(dto.getSoundCard()));
        computer.setGraphicCard(apiDeviceMapper.toDomain(dto.getGraphicCard()));
        computer.setUsb(apiDeviceMapper.toDomain(dto.getUsb()));
        return computer;
    }

    public DesktopComputerDto toDto(DesktopComputer computer) {
        DesktopComputerDto dto = new DesktopComputerDto();
        dto.setId(computer.getId());
        dto.setIdUser(computer.getIdUser());
        dto.setName(computer.getName());
        dto.setActiveStatus(computer.getStatus());
        dto.setInfo(computer.getInfo());
        dto.setProcessor(apiDeviceMapper.toDto(computer.getProcessor()));
        dto.setMotherboard(apiDeviceMapper.toDto(computer.getMotherboard()));
        dto.setRam(apiDeviceMapper.toDto(computer.getRam()));
        dto.setDisk(apiDeviceMapper.toDto(computer.getDisk()));
        dto.setPower(apiDeviceMapper.toDto(computer.getPower()));
        dto.setCooling(apiDeviceMapper.toDto(computer.getCooling()));
        dto.setDisplay(apiDeviceMapper.toDto(computer.getDisplay()));
        dto.setKeyboard(apiDeviceMapper.toDto(computer.getKeyboard()));
        dto.setMouse(apiDeviceMapper.toDto(computer.getMouse()));
        dto.setComputerCase(apiDeviceMapper.toDto(computer.getComputerCase()));
        dto.setSoundCard(apiDeviceMapper.toDto(computer.getSoundCard()));
        dto.setGraphicCard(apiDeviceMapper.toDto(computer.getGraphicCard()));
        dto.setUsb(apiDeviceMapper.toDto(computer.getUsb()));
        return dto;
    }
}
