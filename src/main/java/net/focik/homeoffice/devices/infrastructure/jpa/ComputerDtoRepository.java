package net.focik.homeoffice.devices.infrastructure.jpa;

import net.focik.homeoffice.devices.infrastructure.dto.ComputerDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

interface ComputerDtoRepository extends JpaRepository<ComputerDbDto, Integer> {

}
