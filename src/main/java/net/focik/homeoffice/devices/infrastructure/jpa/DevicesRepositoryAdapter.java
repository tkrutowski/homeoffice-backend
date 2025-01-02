package net.focik.homeoffice.devices.infrastructure.jpa;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.port.secondary.DeviceRepository;
import net.focik.homeoffice.devices.infrastructure.dto.DeviceDbDto;
import net.focik.homeoffice.devices.infrastructure.mapper.JpaDeviceMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class DevicesRepositoryAdapter implements DeviceRepository {

    private final DevicesDtoRepository devicesDtoRepository;
    private final JpaDeviceMapper mapper;

    @Override
    public Device saveDevice(Device device) {
        DeviceDbDto dto = mapper.toDto(device);
        log.debug("Saving device: {}", dto);
        DeviceDbDto saved = devicesDtoRepository.save(dto);
        log.debug("Saved device: {}", saved);
        Device domain = mapper.toDomain(dto);
       log.debug("Mapped saved device to domain: {}", domain);
        return domain;
    }

    @Override
    public Optional<Device> findDeviceById(int id) {
        return devicesDtoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Device> findAllDevices() {
        return devicesDtoRepository.findAll().stream()
                .peek(devicesDto -> log.debug("Found device {}", devicesDto))
                .map(mapper::toDomain)
                .peek(devicesDto -> log.debug("Mapped device {}", devicesDto))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDevice(Integer idDevice) {
        devicesDtoRepository.deleteById(idDevice);
    }
}
