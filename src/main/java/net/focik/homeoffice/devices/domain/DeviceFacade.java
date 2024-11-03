package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.devices.domain.port.primary.DeleteDeviceUseCase;
import net.focik.homeoffice.devices.domain.port.primary.FindDeviceUseCase;
import net.focik.homeoffice.devices.domain.port.primary.SaveDeviceUseCase;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class DeviceFacade implements FindDeviceUseCase, SaveDeviceUseCase, DeleteDeviceUseCase {
    private final DeviceService deviceService;
    private final DeviceTypeService deviceTypeService;

    @Override
    public Computer getComputerBuUser(int idUser) {
        return null;
    }

    @Override
    public Computer getComputerById(int id) {
        return null;
    }

    @Override
    public Device getDeviceById(int id) {
        return deviceService.getDeviceById(id);
    }

    @Override
    public List<Computer> getComputers() {
        return List.of();
    }

    @Override
    public List<Device> getDevices(ActiveStatus activeStatus) {
        return deviceService.getDevices(activeStatus);
    }

    @Override
    public DeviceType findDeviceType(Integer id) {
        return deviceTypeService.getById(id);
    }

    @Override
    public List<DeviceType> getAllDeviceTypes() {
        return deviceTypeService.getDeviceTypes();
    }

    @Override
    public DeviceType add(DeviceType deviceType) {
        return deviceTypeService.add(deviceType);
    }

    @Override
    public Device add(Device device) {
        return deviceService.add(device);
    }

    @Override
    public Device update(Device device) {
        return deviceService.update(device);
    }

    @Override
    public Device updateStatus(Integer idDevice, ActiveStatus status) {
        return deviceService.updateStatus(idDevice, status);
    }

    @Override
    public void deleteDevice(Integer idDevice) {
        deviceService.deleteDevice(idDevice);
    }
}
