package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

interface InvoiceDtoRepository extends JpaRepository<InvoiceDbDto, Integer>, JpaSpecificationExecutor<InvoiceDbDto> {

    List<InvoiceDbDto> findAllByCustomer_Id(Integer customerId);

    Optional<InvoiceDbDto> findByNumber(String number);

    List<InvoiceDbDto> findInvoiceDbDtosByNumberContainsOrderByNumberDesc(String number);

}
