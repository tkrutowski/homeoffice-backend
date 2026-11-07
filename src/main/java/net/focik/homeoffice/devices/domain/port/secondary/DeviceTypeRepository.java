package net.focik.homeoffice.devices.domain.port.secondary;

import net.focik.homeoffice.devices.domain.model.DeviceType;

import java.util.List;
import java.util.Optional;

public interface DeviceTypeRepository {

    DeviceType add(DeviceType deviceType);

    Optional<DeviceType> save(DeviceType series);

    void delete(Integer id);

    Optional<DeviceType> findById(Integer id);

    List<DeviceType> findAll();
}
