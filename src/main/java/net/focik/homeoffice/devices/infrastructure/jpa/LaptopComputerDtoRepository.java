package net.focik.homeoffice.devices.infrastructure.jpa;

import net.focik.homeoffice.devices.infrastructure.dto.LaptopComputerDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LaptopComputerDtoRepository extends JpaRepository<LaptopComputerDbDto, Integer> {
}
