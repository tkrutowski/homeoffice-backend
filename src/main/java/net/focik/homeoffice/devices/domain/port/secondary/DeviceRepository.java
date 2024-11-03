package net.focik.homeoffice.devices.domain.port.secondary;

import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.Device;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface DeviceRepository {
    Computer saveComputer(Computer computer);

    Device saveDevice(Device device);

    Optional<Device> findDeviceById(int id);

    List<Device> findAllDevices();

    List<Computer> findAllComputers();

    List<Computer> findComputersByUser(int userId);

    void deleteDevice(Integer idDevice);
}
