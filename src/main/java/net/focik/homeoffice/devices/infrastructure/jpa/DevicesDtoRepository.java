package net.focik.homeoffice.devices.infrastructure.jpa;

import net.focik.homeoffice.devices.infrastructure.dto.DeviceDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface DevicesDtoRepository extends JpaRepository<DeviceDbDto, Integer> {

    List<DeviceDbDto> findAllByFirm_Id(Integer idFirm);
}
