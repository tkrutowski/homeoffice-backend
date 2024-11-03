package net.focik.homeoffice.devices.infrastructure.jpa;

import net.focik.homeoffice.devices.infrastructure.dto.DeviceTypeDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeviceTypeDtoRepository extends JpaRepository<DeviceTypeDbDto, Integer> {

}
