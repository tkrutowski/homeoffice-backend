package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.DeviceType;

import java.util.List;

public interface FindDeviceTypeUseCase {
    DeviceType findDeviceType(Integer id);

    List<DeviceType> getAllDeviceTypes();
}
