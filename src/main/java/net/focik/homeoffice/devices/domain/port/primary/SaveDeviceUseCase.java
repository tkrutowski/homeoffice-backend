package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.model.DeviceFile;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.util.List;

public interface SaveDeviceUseCase {
    DeviceType add(DeviceType deviceType);

    Device add(Device device);

    Device update(Device device);

    Device updateStatus(Integer idDevice, ActiveStatus status);

    Device addFiles(Integer deviceId, List<DeviceFile> files);
}
