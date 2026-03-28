package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.SupplierDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SupplierDtoRepository extends JpaRepository<SupplierDbDto, Integer> {

    List<SupplierDbDto> findAllByName(String name);

}
