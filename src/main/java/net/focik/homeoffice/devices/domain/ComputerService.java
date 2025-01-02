package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.exception.DeviceNotFoundException;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.port.secondary.ComputerRepository;
import net.focik.homeoffice.utils.FileHelper;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
class ComputerService {
    private final ComputerRepository computerRepository;

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
       return computerRepository.findComputerById(id).orElse(null);
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

//    public void deleteDevice(Integer idDevice) {
//        computerRepository.deleteDevice(idDevice);
//    }

    public Computer updateStatus(Integer id, ActiveStatus status) {
        Optional<Computer> computerById = computerRepository.findComputerById(id);
        if(computerById.isPresent()) {
            computerById.get().setStatus(status);
            return computerRepository.saveComputer(computerById.get());
        }
        throw new DeviceNotFoundException(id);
    }
}
