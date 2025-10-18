package net.focik.homeoffice.devices.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.api.dto.ComputerDto;
import net.focik.homeoffice.devices.domain.model.Computer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiComputerMapper {
    private final ApiDeviceMapper apiDeviceMapper;

    public Computer toDomain(ComputerDto dto) {
        return Computer.builder()
                .id(dto.getId())
                .idUser(dto.getIdUser())
                .disk(apiDeviceMapper.toDomain(dto.getDisk()))
                .computerCase(apiDeviceMapper.toDomain(dto.getComputerCase()))
                .mouse(apiDeviceMapper.toDomain(dto.getMouse()))
                .power(apiDeviceMapper.toDomain(dto.getPower()))
                .ram(apiDeviceMapper.toDomain(dto.getRam()))
                .usb(apiDeviceMapper.toDomain(dto.getUsb()))
                .cooling(apiDeviceMapper.toDomain(dto.getCooling()))
                .computerType(dto.getComputerType())
                .display(apiDeviceMapper.toDomain(dto.getDisplay()))
                .keyboard(apiDeviceMapper.toDomain(dto.getKeyboard()))
                .graphicCard(apiDeviceMapper.toDomain(dto.getGraphicCard()))
                .processor(apiDeviceMapper.toDomain(dto.getProcessor()))
                .motherboard(apiDeviceMapper.toDomain(dto.getMotherboard()))
                .status(dto.getActiveStatus())
                .info(dto.getInfo())
                .soundCard(apiDeviceMapper.toDomain(dto.getSoundCard()))
                .name(dto.getName())
                .build();
    }

    public ComputerDto toDto(Computer computer) {
        return ComputerDto.builder()
                .id(computer.getId())
                .idUser(computer.getIdUser())
                .disk(apiDeviceMapper.toDto(computer.getDisk()))
                .computerCase(apiDeviceMapper.toDto(computer.getComputerCase()))
                .mouse(apiDeviceMapper.toDto(computer.getMouse()))
                .power(apiDeviceMapper.toDto(computer.getPower()))
                .ram(apiDeviceMapper.toDto(computer.getRam()))
                .usb(apiDeviceMapper.toDto(computer.getUsb()))
                .cooling(apiDeviceMapper.toDto(computer.getCooling()))
                .computerType(computer.getComputerType())
                .display(apiDeviceMapper.toDto(computer.getDisplay()))
                .keyboard(apiDeviceMapper.toDto(computer.getKeyboard()))
                .graphicCard(apiDeviceMapper.toDto(computer.getGraphicCard()))
                .processor(apiDeviceMapper.toDto(computer.getProcessor()))
                .motherboard(apiDeviceMapper.toDto(computer.getMotherboard()))
                .activeStatus(computer.getStatus())
                .info(computer.getInfo())
                .soundCard(apiDeviceMapper.toDto(computer.getSoundCard()))
                .name(computer.getName())
                .build();
    }
}