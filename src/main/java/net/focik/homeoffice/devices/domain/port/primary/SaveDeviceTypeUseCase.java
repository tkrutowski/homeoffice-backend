package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.DeviceType;

public interface SaveDeviceTypeUseCase {
    DeviceType add(DeviceType deviceType);
}
