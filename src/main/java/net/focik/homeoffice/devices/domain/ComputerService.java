package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.exception.DeviceNotFoundException;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.ComputerType;
import net.focik.homeoffice.devices.domain.model.DesktopComputer;
import net.focik.homeoffice.devices.domain.model.LaptopComputer;
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

    public List<? extends Computer> getComputers(ActiveStatus activeStatus) {
        List<? extends Computer> allComputers = computerRepository.findAllComputers();
        if(activeStatus == ActiveStatus.ALL) {
            allComputers.forEach(this::getComputerFullData);
            return allComputers;
        }
        return allComputers.stream()
                .filter(device -> device.getStatus().equals(activeStatus))
                .peek(this::getComputerFullData)
                .toList();
    }

    public Computer getComputerById(int id, ComputerType type) {
        log.debug("Getting computer by id: {} with type: {}", id, type);
        Optional<? extends Computer> computerById = computerRepository.findComputerById(id, type);
        if(computerById.isEmpty()) {
            log.warn("Computer not found with id: {} and type: {}", id, type);
            return null;
        }
        Computer computer = computerById.get();

        getComputerFullData(computer);

        return computer;
    }

    private ComputerType getComputerType(Computer computer) {
        if (computer instanceof DesktopComputer) {
            return ComputerType.DESKTOP;
        } else if (computer instanceof LaptopComputer) {
            return ComputerType.LAPTOP;
        } else {
            log.warn("Unknown computer type: {}", computer.getClass().getName());
            return ComputerType.DESKTOP; // default fallback
        }
    }

    private void getComputerFullData(Computer computer) {
        if (computer instanceof DesktopComputer desktop) {
            fillDesktopDeviceData(desktop);
        }
        // LaptopComputer nie ma Device pól, więc nic nie robimy
    }

    private void fillDesktopDeviceData(DesktopComputer desktop) {
        if (desktop.getProcessor() != null) {
            desktop.setProcessor(deviceService.getDeviceById(desktop.getProcessor().getId()));
        }
        if (desktop.getMotherboard() != null) {
            desktop.setMotherboard(deviceService.getDeviceById(desktop.getMotherboard().getId()));
        }
        if (!desktop.getRam().isEmpty()) {
            desktop.setRam(desktop.getRam().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (!desktop.getDisk().isEmpty()) {
            desktop.setDisk(desktop.getDisk().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (desktop.getPower() != null) {
            desktop.setPower(deviceService.getDeviceById(desktop.getPower().getId()));
        }
        if (!desktop.getCooling().isEmpty()) {
            desktop.setCooling(desktop.getCooling().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (!desktop.getDisplay().isEmpty()) {
            desktop.setDisplay(desktop.getDisplay().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (desktop.getKeyboard() != null) {
            desktop.setKeyboard(deviceService.getDeviceById(desktop.getKeyboard().getId()));
        }
        if (desktop.getMouse() != null) {
            desktop.setMouse(deviceService.getDeviceById(desktop.getMouse().getId()));
        }
        if (desktop.getComputerCase() != null) {
            desktop.setComputerCase(deviceService.getDeviceById(desktop.getComputerCase().getId()));
        }
        if (desktop.getSoundCard() != null) {
            desktop.setSoundCard(deviceService.getDeviceById(desktop.getSoundCard().getId()));
        }
        if (desktop.getGraphicCard() != null) {
            desktop.setGraphicCard(desktop.getGraphicCard().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
        if (desktop.getUsb() != null) {
            desktop.setUsb(desktop.getUsb().stream()
                    .map(device -> deviceService.getDeviceById(device.getId()))
                    .toList());
        }
    }

    public Computer add(Computer computer) {
        log.debug("Adding computer {}", computer);
        Computer savedComputer = computerRepository.saveComputer(computer);
        getComputerFullData(savedComputer);
        return savedComputer;
    }

    public Computer update(Computer device) {
        log.debug("Updating device {}", device);

        Computer savedDevice = computerRepository.saveComputer(device);
        getComputerFullData(savedDevice);
        log.debug("Updated device {}", savedDevice);
        return savedDevice;
    }

    public Computer updateStatus(Integer id, ActiveStatus status, ComputerType type) {
        log.debug("Updating computer status with id: {} to status: {} with type: {}", id, status, type);
        Optional<? extends Computer> computerById = computerRepository.findComputerById(id, type);
        if(computerById.isPresent()) {
            computerById.get().setStatus(status);
            Computer updatedComputer = computerRepository.saveComputer(computerById.get());
            getComputerFullData(updatedComputer);
            return updatedComputer;
        }
        throw new DeviceNotFoundException(id);
    }

    public void deleteComputer(Integer idComputer) {
        computerRepository.deleteComputer(idComputer);
    }
}
