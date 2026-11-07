package net.focik.homeoffice.devices.domain.service;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.devices.domain.port.primary.FindDeviceTypeUseCase;
import net.focik.homeoffice.devices.domain.port.primary.SaveDeviceTypeUseCase;
import net.focik.homeoffice.devices.domain.port.secondary.DeviceTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class DeviceTypeService implements FindDeviceTypeUseCase, SaveDeviceTypeUseCase {
    private final DeviceTypeRepository deviceTypeRepository;

    @Override
    public List<DeviceType> getAllDeviceTypes() {
        return deviceTypeRepository.findAll();
    }

    @Override
    public DeviceType findDeviceType(Integer id) {
        return deviceTypeRepository.findById(id).orElse(null);
    }

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "DeviceType")
    public DeviceType add(DeviceType deviceType) {
        return deviceTypeRepository.add(deviceType);
    }
}
