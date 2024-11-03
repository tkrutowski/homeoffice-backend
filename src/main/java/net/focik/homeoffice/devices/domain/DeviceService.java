package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.devices.domain.exception.DeviceNotFoundException;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.port.secondary.DeviceRepository;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
class DeviceService {
    private final DeviceRepository deviceRepository;

    public List<Device> getDevices(ActiveStatus activeStatus) {
        List<Device> allDevices = deviceRepository.findAllDevices();
        if(activeStatus == ActiveStatus.ALL) {
            return allDevices;
        }
        return allDevices.stream()
                .filter(device -> device.getActiveStatus().equals(activeStatus))
                .toList();
    }

    public Device getDeviceById(int id) {
       return deviceRepository.findDeviceById(id).orElse(null);
    }

    public Device add(Device device) {
        return deviceRepository.saveDevice(device);
    }

    public Device update(Device device) {
        return deviceRepository.saveDevice(device);
    }

    public void deleteDevice(Integer idDevice) {
        deviceRepository.deleteDevice(idDevice);
    }

    public Device updateStatus(Integer idDevice, ActiveStatus status) {
        Optional<Device> deviceById = deviceRepository.findDeviceById(idDevice);
        if(deviceById.isPresent()) {
            deviceById.get().setActiveStatus(status);
            return deviceRepository.saveDevice(deviceById.get());
        }
        throw new DeviceNotFoundException(idDevice);
    }
}
