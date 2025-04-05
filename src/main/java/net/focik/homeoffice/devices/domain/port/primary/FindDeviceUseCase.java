package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.util.List;

public interface FindDeviceUseCase {
    Device getDeviceById(int id);

    List<Device> getDevices(ActiveStatus activeStatus);

    DeviceType findDeviceType(Integer id);

    List<DeviceType> getAllDeviceTypes();

    List<Device> getDevicesByFirm(Integer id);
}
