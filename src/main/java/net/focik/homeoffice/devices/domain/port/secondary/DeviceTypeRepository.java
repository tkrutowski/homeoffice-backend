package net.focik.homeoffice.devices.domain.port.secondary;

import net.focik.homeoffice.devices.domain.model.DeviceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface DeviceTypeRepository {

    DeviceType add(DeviceType deviceType);

    Optional<DeviceType> save(DeviceType series);

    void delete(Integer id);

    Optional<DeviceType> findById(Integer id);

    List<DeviceType> findAll();
}
