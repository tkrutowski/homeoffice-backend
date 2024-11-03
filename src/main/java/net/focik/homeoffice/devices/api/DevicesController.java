package net.focik.homeoffice.devices.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.api.dto.DeviceDto;
import net.focik.homeoffice.devices.api.mapper.ApiDeviceMapper;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.port.primary.DeleteDeviceUseCase;
import net.focik.homeoffice.devices.domain.port.primary.FindDeviceUseCase;
import net.focik.homeoffice.devices.domain.port.primary.SaveDeviceUseCase;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/devices")
public class DevicesController {
    private final FindDeviceUseCase findDeviceUseCase;
    private final SaveDeviceUseCase saveDeviceUseCase;
    private final DeleteDeviceUseCase deleteDeviceUseCase;
    private final ApiDeviceMapper apiDeviceMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DEVICE_READ_ALL','DEVICE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<DeviceDto>> getDevices(@RequestParam(value = "status", defaultValue = "ALL") ActiveStatus activeStatus) {
        log.info("Request to get devices with status: {}.",activeStatus);
        List<Device> devices = findDeviceUseCase.getDevices(activeStatus);

        if (devices.isEmpty()) {
            log.warn("No devices found.");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.info("Found {} devices with status {}.", devices.size(), activeStatus);
        return new ResponseEntity<>(devices.stream()
                .peek(device -> log.debug("Found device {}", device))
                .map(apiDeviceMapper::toDto)
                .peek(deviceDto -> log.debug("Mapped found device {}", deviceDto))
                .toList(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEVICE_READ_ALL','DEVICE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<DeviceDto> getById(@PathVariable int id) {
        log.info("Request to get device by id: {}", id);
        Device device = findDeviceUseCase.getDeviceById(id);
        if (device == null) {
            log.warn("No device found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Device found: {}", device);
        DeviceDto dto = apiDeviceMapper.toDto(device);
        log.debug("Mapped to Device dto: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('DEVICE_WRITE_ALL','DEVICE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeviceDto> addDevice(@RequestBody DeviceDto deviceDto) {
        log.info("Request to add a new device received with data: {}", deviceDto);

        Device deviceToAdd = apiDeviceMapper.toDomain(deviceDto);
        log.debug("Mapped Device DTO to domain object: {}", deviceToAdd);

        Device deviceAdded = saveDeviceUseCase.add(deviceToAdd);
        log.info("Device added successfully: {}", deviceAdded);

        DeviceDto dto = apiDeviceMapper.toDto(deviceAdded);
        log.debug("Mapped Device DTO to domain object: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('DEVICE_WRITE_ALL','DEVICE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeviceDto> editDevice(@RequestBody DeviceDto deviceDto) {
        log.info("Request to edit a device received with data: {}", deviceDto);

        Device deviceToUpdate = apiDeviceMapper.toDomain(deviceDto);
        log.debug("Mapped Device DTO to domain object: {}", deviceToUpdate);

        Device updatedDevice = saveDeviceUseCase.update(deviceToUpdate);
        log.info("Device updated successfully: {}", updatedDevice);

        DeviceDto dto = apiDeviceMapper.toDto(updatedDevice);
        log.debug("Mapped Device DTO to domain object: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEVICE_DELETE_ALL','DEVICE_DELETE') or hasRole('ROLE_ADMIN')")
    public void deleteDevice(@PathVariable Integer id) {
        log.info("Request to delete device with id: {}", id);
        deleteDeviceUseCase.deleteDevice(id);
        log.info("Device with id: {} deleted successfully", id);
    }


    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyAuthority('DEVICE_WRITE_ALL','DEVICE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeviceDto> updateStatus(@PathVariable int id, @RequestParam ActiveStatus status) {
        log.info("Request to update status with id: {}", id);

        Device updatedDevice = saveDeviceUseCase.updateStatus(id, status);
        log.info("Device updated successfully: {}", updatedDevice);

        DeviceDto dto = apiDeviceMapper.toDto(updatedDevice);
        log.debug("Mapped Device DTO to domain object: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}
