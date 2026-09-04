package net.focik.homeoffice.devices.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.api.dto.LaptopComputerDto;
import net.focik.homeoffice.devices.domain.model.LaptopComputer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiLaptopComputerMapper {

    public LaptopComputer toDomain(LaptopComputerDto dto) {
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

    public LaptopComputerDto toDto(LaptopComputer computer) {
        LaptopComputerDto dto = new LaptopComputerDto();
        dto.setId(computer.getId());
        dto.setIdUser(computer.getIdUser());
        dto.setName(computer.getName());
        dto.setCpu(computer.getCpu());
        dto.setGpu(computer.getGpu());
        dto.setRam(computer.getRam());
        dto.setStorage(computer.getStorage());
        dto.setDisplay(computer.getDisplay());
        dto.setActiveStatus(computer.getStatus());
        dto.setInfo(computer.getInfo());
        return dto;
    }
}
