package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.FirmDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FirmDtoRepository extends JpaRepository<FirmDbDto, Integer> {

    Optional<FirmDbDto> findByName(String name);
}
