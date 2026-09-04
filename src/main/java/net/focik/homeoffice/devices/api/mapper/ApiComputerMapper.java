package net.focik.homeoffice.devices.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.api.dto.ComputerDto;
import net.focik.homeoffice.devices.api.dto.DesktopComputerDto;
import net.focik.homeoffice.devices.api.dto.LaptopComputerDto;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.DesktopComputer;
import net.focik.homeoffice.devices.domain.model.LaptopComputer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiComputerMapper {
    private final ApiDeviceMapper apiDeviceMapper;
    private final ApiDesktopComputerMapper apiDesktopComputerMapper;
    private final ApiLaptopComputerMapper apiLaptopComputerMapper;

    public Computer toDomain(ComputerDto dto) {
        if (dto instanceof DesktopComputerDto desktopDto) {
            return apiDesktopComputerMapper.toDomain(desktopDto);
        } else if (dto instanceof LaptopComputerDto laptopDto) {
            return apiLaptopComputerMapper.toDomain(laptopDto);
        } else {
            throw new IllegalArgumentException("Unknown computer DTO type: " + dto.getClass().getName());
        }
    }

    public ComputerDto toDto(Computer computer) {
        if (computer instanceof DesktopComputer desktop) {
            return apiDesktopComputerMapper.toDto(desktop);
        } else if (computer instanceof LaptopComputer laptop) {
            return apiLaptopComputerMapper.toDto(laptop);
        } else {
            throw new IllegalArgumentException("Unknown computer type: " + computer.getClass().getName());
        }
    }
}