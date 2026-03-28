package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.CostDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
interface CostDtoRepository extends JpaRepository<CostDbDto, Integer>, JpaSpecificationExecutor<CostDbDto> {
    List<CostDbDto> findByInvoiceDate(LocalDate date);
}
