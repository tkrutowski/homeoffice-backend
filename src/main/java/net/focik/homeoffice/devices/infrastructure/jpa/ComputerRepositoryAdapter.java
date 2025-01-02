package net.focik.homeoffice.devices.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.port.secondary.ComputerRepository;
import net.focik.homeoffice.devices.infrastructure.dto.ComputerDbDto;
import net.focik.homeoffice.devices.infrastructure.mapper.JpaComputerMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComputerRepositoryAdapter implements ComputerRepository {

    private final ComputerDtoRepository computerDtoRepository;
    private final JpaComputerMapper mapper;
    private final DevicesRepositoryAdapter devicesRepositoryAdapter;


    @Override
    public Computer saveComputer(Computer computer) {
        ComputerDbDto dto = mapper.toDto(computer);
        log.debug("Saving computer: {}", dto);
        ComputerDbDto saved = computerDtoRepository.save(dto);
        log.debug("Saved computer: {}", saved);
        Computer domain = mapToDomain(dto);
        log.debug("Mapped saved computer to domain: {}", domain);
        return domain;
    }


    @Override
    public Optional<Computer> findComputerById(int id) {
        return computerDtoRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<Computer> findAllComputers() {
        return computerDtoRepository.findAll().stream()
                .peek(dbDto -> log.debug("Found computer {}", dbDto))
                .map(this::mapToDomain)
                .peek(computer -> log.debug("Mapped computer {}", computer))
                .collect(Collectors.toList());
    }


    @Override
    public List<Computer> findComputersByUser(int userId) {
        return List.of();
    }

    @Override
    public void deleteComputer(Integer id) {
        computerDtoRepository.deleteById(id);
    }

    private Computer mapToDomain(ComputerDbDto dto) {
        return Computer.builder()
                .id(dto.getId())
                .idUser(dto.getIdUser())
                .disk(getDeviceFromList(dto.getDisk()))
                .computerCase(getDeviceById(dto.getComputerCase()))
                .mouse(getDeviceById(dto.getMouse()))
                .power(getDeviceById(dto.getPower()))
                .ram(getDeviceFromList(dto.getRam()))
                .usb(getDeviceFromList(dto.getUsb()))
                .cooling(getDeviceFromList(dto.getCooling()))
                .computerType(dto.getComputerType())
                .display(getDeviceFromList(dto.getDisplay()))
                .keyboard(getDeviceById(dto.getKeyboard()))
                .graphicCard(getDeviceFromList(dto.getGraphicCard()))
                .processor(getDeviceById(dto.getProcessor()))
                .motherboard(getDeviceById(dto.getMotherboard()))
                .status(dto.getActiveStatus())
                .info(dto.getInfo())
                .soundCard(getDeviceById(dto.getSoundCard()))
                .name(dto.getName())
                .build();
    }

    private Device getDeviceById(Integer input) {
        if (input == null) {
            log.warn("Input is null.");
            return null;
        }
        log.info("Processing input: {}", input);
        Optional<Device> deviceById = devicesRepositoryAdapter.findDeviceById(input);
        if (deviceById.isPresent()) {
            log.debug("Found device: {}", deviceById.get());
            return deviceById.get();
        }
        return null;
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
            Optional<Device> deviceById = devicesRepositoryAdapter.findDeviceById(Integer.parseInt(id));
            if (deviceById.isPresent()) {
                log.debug("Found device: {}", deviceById.get());
                deviceList.add(deviceById.get());
            }
        }
        return deviceList;
    }
}
