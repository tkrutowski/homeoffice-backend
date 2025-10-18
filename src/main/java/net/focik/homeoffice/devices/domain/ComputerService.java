package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.exception.DeviceNotFoundException;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.port.secondary.ComputerRepository;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
class ComputerService {
    private final ComputerRepository computerRepository;
    private final DeviceService deviceService;

    public List<Computer> getComputers(ActiveStatus activeStatus) {
        List<Computer> allComputers = computerRepository.findAllComputers();
        if(activeStatus == ActiveStatus.ALL) {
            return allComputers;
        }
        return allComputers.stream()
                .filter(device -> device.getStatus().equals(activeStatus))
                .toList();
    }

    public Computer getComputerById(int id) {

        Optional<Computer> computerById = computerRepository.findComputerById(id);
        if(computerById.isEmpty()) {
            return null;
        }
        Computer computer = computerById.get();

        if (computer.getProcessor() != null) {
            computer.setProcessor(deviceService.getDeviceById(computer.getProcessor().getId()));
        }
        if (computer.getMotherboard() != null) {
            computer.setMotherboard(deviceService.getDeviceById(computer.getMotherboard().getId()));
        }
        if (!computer.getRam().isEmpty()) {
            computer.setRam(computer.getRam().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (!computer.getDisk().isEmpty()) {
            computer.setDisk(computer.getDisk().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (computer.getPower() != null) {
            computer.setPower(deviceService.getDeviceById(computer.getPower().getId()));
        }
        if (!computer.getCooling().isEmpty()) {
            computer.setCooling(computer.getCooling().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (!computer.getDisplay().isEmpty()) {
            computer.setDisplay(computer.getDisplay().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (computer.getKeyboard() != null) {
            computer.setKeyboard(deviceService.getDeviceById(computer.getKeyboard().getId()));
        }
        if (computer.getMouse() != null) {
            computer.setMouse(deviceService.getDeviceById(computer.getMouse().getId()));
        }
        if (computer.getComputerCase() != null) {
            computer.setComputerCase(deviceService.getDeviceById(computer.getComputerCase().getId()));
        }
        if (computer.getSoundCard() != null) {
            computer.setSoundCard(deviceService.getDeviceById(computer.getSoundCard().getId()));
        }
        if (computer.getGraphicCard() != null) {
            computer.setGraphicCard(computer.getGraphicCard().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (computer.getUsb() != null) {
            computer.setUsb(computer.getUsb().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }

        return computer;
    }

    public Computer add(Computer computer) {
        log.debug("Adding computer {}", computer);
        return computerRepository.saveComputer(computer);
    }

    public Computer update(Computer device) {
        log.debug("Updating device {}", device);

        Computer savedDevice = computerRepository.saveComputer(device);
        log.debug("Updated device {}", savedDevice);
        return savedDevice;
    }

    public Computer updateStatus(Integer id, ActiveStatus status) {
        Optional<Computer> computerById = computerRepository.findComputerById(id);
        if(computerById.isPresent()) {
            computerById.get().setStatus(status);
            return computerRepository.saveComputer(computerById.get());
        }
        throw new DeviceNotFoundException(id);
    }

    public void deleteComputer(Integer idComputer) {
        computerRepository.deleteComputer(idComputer);
    }
}
