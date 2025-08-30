package net.focik.homeoffice.devices.infrastructure.jpa;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.port.secondary.DeviceRepository;
import net.focik.homeoffice.devices.infrastructure.dto.DeviceDbDto;
import net.focik.homeoffice.devices.infrastructure.mapper.JpaDeviceMapper;
import net.focik.homeoffice.fileService.infrastructure.dto.FileInfoDbDto;
import net.focik.homeoffice.fileService.infrastructure.jpa.FilesDtoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class DevicesRepositoryAdapter implements DeviceRepository {

    private final DevicesDtoRepository devicesDtoRepository;
    private final FilesDtoRepository fileInfoDbDtoRepository;
    private final JpaDeviceMapper mapper;

    @Override
    @Transactional
    public Device saveDevice(Device device) {
        DeviceDbDto dto = mapper.toDto(device);

        // Najpierw pobierz istniejące pliki, jeśli to aktualizacja
        if (dto.getId() != null) {
            if (dto.getId() != 0) {
                DeviceDbDto existing = devicesDtoRepository.findById(dto.getId()).orElse(null);
                deleteRemovedFiles(dto, existing);
            } else {
                dto.setId(null);
            }
        }

        if (dto.getFiles() != null) {
            Set<FileInfoDbDto> savedFiles = dto.getFiles().stream()
                    .map(fileInfoDbDtoRepository::save)
                    .collect(Collectors.toSet());
            dto.setFiles(savedFiles);
        }

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
    public List<Device> findAllDevicesByFirm(Integer idFirm) {
        return devicesDtoRepository.findAllByFirm_Id(idFirm).stream()
                .peek(devicesDto -> log.debug("Found device {}", devicesDto))
                .map(mapper::toDomain)
                .peek(devicesDto -> log.debug("Mapped device {}", devicesDto))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDevice(Integer idDevice) {
        devicesDtoRepository.deleteById(idDevice);
    }

    private void deleteRemovedFiles(DeviceDbDto newDevice, DeviceDbDto existingDevice) {
        if (existingDevice != null && existingDevice.getFiles() != null) {
            var filesToDelete = existingDevice.getFiles().stream()
                    .filter(file -> isFileRemoved(file, newDevice.getFiles()))
                    .toList();

            filesToDelete.forEach(fileInfoDbDto -> fileInfoDbDtoRepository.deleteById(fileInfoDbDto.getId()));
        }
    }

    private boolean isFileRemoved(FileInfoDbDto existingFile, Set<FileInfoDbDto> newFiles) {
        return newFiles == null ||
                newFiles.stream().noneMatch(newFile -> newFile.getId().equals(existingFile.getId()));
    }

}
