package net.focik.homeoffice.devices.infrastructure.mapper;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.LaptopComputer;
import net.focik.homeoffice.devices.infrastructure.dto.LaptopComputerDbDto;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JpaLaptopComputerMapper {

    public LaptopComputerDbDto toDto(LaptopComputer computer) {
        LaptopComputerDbDto dto = new LaptopComputerDbDto();
        dto.setId(computer.getId());
        dto.setIdUser(computer.getIdUser());
        dto.setName(computer.getName());
        dto.setCpu(computer.getCpu());
        dto.setGpu(computer.getGpu());
        dto.setRam(computer.getRam());
        dto.setStorage(computer.getStorage());
        dto.setDisplay(computer.getDisplay());
        dto.setActiveStatus(computer.getStatus() != null ? computer.getStatus() : null);
        dto.setInfo(computer.getInfo() != null ? computer.getInfo() : null);
        return dto;
    }

    public LaptopComputer toDomain(LaptopComputerDbDto dto) {
        LaptopComputer computer = new LaptopComputer();
        computer.setId(dto.getId());
        computer.setIdUser(dto.getIdUser());
        computer.setName(dto.getName());
        computer.setCpu(dto.getCpu());
        computer.setGpu(dto.getGpu());
        computer.setRam(dto.getRam());
        computer.setStorage(dto.getStorage());
        computer.setDisplay(dto.getDisplay());
        computer.setStatus(dto.getActiveStatus());
        computer.setInfo(dto.getInfo());
        return computer;
    }
}
