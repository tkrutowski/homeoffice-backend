package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.BankDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankDtoRepository extends JpaRepository<BankDbDto, Integer> {

    Optional<BankDbDto> findByName(String name);
}
