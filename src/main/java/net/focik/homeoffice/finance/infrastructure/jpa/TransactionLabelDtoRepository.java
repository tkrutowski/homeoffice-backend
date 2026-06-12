package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.TransactionLabelDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLabelDtoRepository extends JpaRepository<TransactionLabelDbDto, Integer> {
    Optional<TransactionLabelDbDto> findByName(String name);
}
