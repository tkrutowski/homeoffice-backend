package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.devices.domain.port.secondary.DeviceTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
class DeviceTypeService {
    private final DeviceTypeRepository deviceTypeRepository;

    public List<DeviceType> getDeviceTypes() {
        return deviceTypeRepository.findAll();
    }

    public DeviceType getById(Integer id) {
        return deviceTypeRepository.findById(id).orElse(null);
    }

    public DeviceType add(DeviceType deviceType) {
        return deviceTypeRepository.add(deviceType);
    }
}
