package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

interface InvoiceItemDtoRepository extends JpaRepository<InvoiceItemDbDto, Long> {

}
