package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategoryType;
import net.focik.homeoffice.finance.infrastructure.dto.TransactionCategoryDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionCategoryDtoRepository extends JpaRepository<TransactionCategoryDbDto, Integer> {
    Optional<TransactionCategoryDbDto> findByNameContainingIgnoreCase(String name);
    Optional<TransactionCategoryDbDto> findByNameContainingIgnoreCaseAndType(String name, TransactionCategoryType type);
}
