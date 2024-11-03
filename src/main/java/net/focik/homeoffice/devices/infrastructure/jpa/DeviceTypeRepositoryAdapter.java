package net.focik.homeoffice.devices.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.devices.domain.port.secondary.DeviceTypeRepository;
import net.focik.homeoffice.devices.infrastructure.dto.DeviceTypeDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTypeRepositoryAdapter implements DeviceTypeRepository {

    private final DeviceTypeDtoRepository deviceTypeDtoRepository;
    private final ModelMapper mapper;

    @Override
    public DeviceType add(DeviceType deviceType) {
        DeviceTypeDbDto saved = deviceTypeDtoRepository.save(mapper.map(deviceType, DeviceTypeDbDto.class));
        return mapper.map(saved, DeviceType.class);
    }

    @Override
    public Optional<DeviceType> save(DeviceType deviceType) {
        DeviceTypeDbDto dbDto = deviceTypeDtoRepository.save(mapper.map(deviceType, DeviceTypeDbDto.class));
        return Optional.of(mapper.map(dbDto, DeviceType.class));
    }

    @Override
    public void delete(Integer id) {
        deviceTypeDtoRepository.deleteById(id);
    }

    @Override
    public Optional<DeviceType> findById(Integer id) {
        return deviceTypeDtoRepository.findById(id)
                .map(typeDbDto -> mapper.map(typeDbDto, DeviceType.class));
    }

    @Override
    public List<DeviceType> findAll() {
        return deviceTypeDtoRepository.findAll().stream()
                .peek(deviceTypeDbDto -> log.debug("Found device type: {}", deviceTypeDbDto))
                .map(typeDbDto -> mapper.map(typeDbDto, DeviceType.class))
                .peek(deviceType -> log.debug("Mapped device type: {}", deviceType))
                .toList();
    }
}