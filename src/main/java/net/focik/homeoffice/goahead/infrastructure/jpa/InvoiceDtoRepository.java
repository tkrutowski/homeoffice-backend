package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface InvoiceDtoRepository extends JpaRepository<InvoiceDbDto, Integer> {

    List<InvoiceDbDto> findAllByIdCustomer(Integer idCustomer);

    Optional<InvoiceDbDto> findByNumber(String number);
}
