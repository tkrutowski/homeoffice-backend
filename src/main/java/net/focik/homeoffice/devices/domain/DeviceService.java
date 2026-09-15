package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.exception.DeviceCanNotBeDeletedException;
import net.focik.homeoffice.devices.domain.exception.DeviceNotFoundException;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.model.DesktopComputer;
import net.focik.homeoffice.devices.domain.port.secondary.ComputerRepository;
import net.focik.homeoffice.devices.domain.port.secondary.DeviceRepository;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.utils.share.ActiveStatus;
import net.focik.homeoffice.utils.share.Module;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
class DeviceService {
    private final DeviceRepository deviceRepository;
    private final FileRepository fileRepository;
    private final ComputerRepository computerRepository;

    public List<Device> getDevices(ActiveStatus activeStatus) {
        List<Device> allDevices = deviceRepository.findAllDevices();
        if (activeStatus == ActiveStatus.ALL) {
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
        log.debug("Adding device {}", device);
        if (StringUtils.isEmpty(device.getImageUrl())) {
            log.debug("No image url found for device {}", device);
        } else {
            log.debug("Image url found for device {}", device);
            device.setImageUrl(fileRepository.downloadAndSaveImage(device.getImageUrl(), device.getName(), Module.DEVICE_IMAGES));
        }
        return deviceRepository.saveDevice(device);
    }

    public Device update(Device device) {
        log.debug("Updating device {}", device);

        if (device.getImageUrl() != null && !device.getImageUrl().contains("focik-home")) {
            device.setImageUrl(fileRepository.downloadAndSaveImage(device.getImageUrl(), device.getName(), Module.DEVICE_IMAGES));
        }
        Device savedDevice = deviceRepository.saveDevice(device);
        log.debug("Updated device {}", savedDevice);
        return savedDevice;
    }

    public void deleteDevice(Integer idDevice) {
        long computersUsingDevice = computerRepository.findAllComputers().stream()
                .filter(computer -> computer instanceof DesktopComputer)
                .map(computer -> (DesktopComputer) computer)
                .filter(desktop -> isDeviceUsedInComputer(desktop, idDevice))
                .count();
        if (computersUsingDevice > 0) {
            log.warn("Device with ID {} cannot be deleted — used as a component in {} computer(s).", idDevice, computersUsingDevice);
            throw new DeviceCanNotBeDeletedException("komputer, w którym jest użyte jako podzespół. (" + computersUsingDevice + ")");
        }
        deviceRepository.deleteDevice(idDevice);
    }

    private boolean isDeviceUsedInComputer(DesktopComputer desktop, Integer idDevice) {
        return Stream.of(
                        Stream.of(desktop.getProcessor(), desktop.getMotherboard(), desktop.getPower(),
                                        desktop.getKeyboard(), desktop.getMouse(), desktop.getComputerCase(), desktop.getSoundCard())
                                .filter(Objects::nonNull),
                        Stream.of(desktop.getRam(), desktop.getDisk(), desktop.getCooling(), desktop.getDisplay(),
                                        desktop.getGraphicCard(), desktop.getUsb())
                                .filter(Objects::nonNull)
                                .flatMap(List::stream)
                )
                .flatMap(stream -> stream)
                .anyMatch(device -> Objects.equals(device.getId(), idDevice));
    }

    public Device updateStatus(Integer idDevice, ActiveStatus status) {
        Optional<Device> deviceById = deviceRepository.findDeviceById(idDevice);
        if (deviceById.isPresent()) {
            deviceById.get().setActiveStatus(status);
            return deviceRepository.saveDevice(deviceById.get());
        }
        throw new DeviceNotFoundException(idDevice);
    }

    public List<Device> getDevicesByFirm(Integer idFirm) {
        return deviceRepository.findAllDevicesByFirm(idFirm);
    }
}
