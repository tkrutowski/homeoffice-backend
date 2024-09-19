package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface InvoiceItemDtoRepository extends JpaRepository<InvoiceItemDbDto, Long> {

    List<InvoiceItemDbDto> findAllByIdInvoice(Integer invoiceId);

    void deleteAllByIdInvoice(Integer id);
}
