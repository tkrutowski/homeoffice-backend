package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.util.List;

public interface FindDeviceUseCase {
    Computer getComputerBuUser(int idUser);

    Computer getComputerById(int id);

    Device getDeviceById(int id);

    List<Computer> getComputers();

    List<Device> getDevices(ActiveStatus activeStatus);

    DeviceType findDeviceType(Integer id);

    List<DeviceType> getAllDeviceTypes();
}
