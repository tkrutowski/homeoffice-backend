package net.focik.homeoffice.devices.domain.port.secondary;

import net.focik.homeoffice.devices.domain.model.Device;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository {

    Device saveDevice(Device device);

    Optional<Device> findDeviceById(int id);

    List<Device> findAllDevices();

    List<Device> findAllDevicesByFirm(Integer idFirm);

    void deleteDevice(Integer idDevice);
}
