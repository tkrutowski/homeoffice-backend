package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface CustomerDtoRepository extends JpaRepository<CustomerDbDto, Integer> {

    List<CustomerDbDto> findAllByName(String name);

    // NIP bywa zapisany z myslnikami i bez - porownujemy wersje znormalizowane
    @Query("select c from CustomerDbDto c where replace(c.nip, '-', '') = :nip")
    List<CustomerDbDto> findAllByNormalizedNip(@Param("nip") String nip);

//    List<CustomerDbDto> findAllByIsActive(Boolean isActive);
}
