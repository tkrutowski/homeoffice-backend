package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CustomerDtoRepository extends JpaRepository<CustomerDbDto, Integer> {

    List<CustomerDbDto> findAllByName(String name);

//    List<CustomerDbDto> findAllByIsActive(Boolean isActive);
}
