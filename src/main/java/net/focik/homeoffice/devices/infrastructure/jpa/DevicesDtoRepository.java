package net.focik.homeoffice.devices.infrastructure.jpa;

import net.focik.homeoffice.devices.infrastructure.dto.DeviceDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

interface DevicesDtoRepository extends JpaRepository<DeviceDbDto, Integer> {

}
