package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.util.List;

public interface FindDeviceUseCase {
    Device getDeviceById(int id);

    List<Device> getDevices(ActiveStatus activeStatus);

    List<Device> getDevicesByFirm(Integer id);
}
