package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.CompanyDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

interface CompanyDtoRepository extends JpaRepository<CompanyDbDto, String> {
}
