package net.focik.homeoffice.devices.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.devices.domain.port.primary.FindDeviceUseCase;
import net.focik.homeoffice.devices.domain.port.primary.SaveDeviceUseCase;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/devices/type")
public class DeviceTypeController {

    private final FindDeviceUseCase findDeviceTypeUseCase;
    private final SaveDeviceUseCase saveDeviceUseCase;
    private final ModelMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DEVICE_READ_ALL','DEVICE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<DeviceType>> getAllDeviceTypes() {
        log.info("Request to get all device types");
        List<DeviceType> deviceTypes = findDeviceTypeUseCase.getAllDeviceTypes();
        log.info("Found {} device types", deviceTypes.size());
        return new ResponseEntity<>(deviceTypes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEVICE_READ_ALL','DEVICE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<DeviceType> getDeviceType(@PathVariable Integer id) {
        log.info("Request to get device type {}", id);
        DeviceType deviceType = findDeviceTypeUseCase.findDeviceType(id);
        log.info("Found device type {}", deviceType);
        return new ResponseEntity<>(deviceType, HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('DEVICE_WRITE_ALL','DEVICE_WRITE') or hasRole('ROLE_ADMIN')")
    public  ResponseEntity<DeviceType> addDeviceType(@RequestBody DeviceType deviceType) {
        log.info("Request to add device type {}", deviceType);
        DeviceType added = saveDeviceUseCase.add(deviceType);
        log.info("Added device type {}", added);
        return new ResponseEntity<>(added,HttpStatus.CREATED);
    }
}