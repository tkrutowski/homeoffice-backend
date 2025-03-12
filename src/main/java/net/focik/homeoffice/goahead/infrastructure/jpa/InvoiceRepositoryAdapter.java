package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final InvoiceDtoRepository invoiceDtoRepository;
    private final ModelMapper mapper;

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceDbDto dbDto = mapper.map(invoice, InvoiceDbDto.class);
        if (dbDto.getInvoiceItems() != null) {
            dbDto.getInvoiceItems().forEach(invoiceItemDto -> invoiceItemDto.setInvoice(dbDto));
        }
        InvoiceDbDto saved = invoiceDtoRepository.save(dbDto);
        return mapper.map(saved, Invoice.class);
    }

    @Override
    public void deleteInvoice(Integer id) {
        invoiceDtoRepository.deleteById(id);
    }

    @Override
    public List<Invoice> findAll() {
        List<InvoiceDbDto> all = invoiceDtoRepository.findAll();
        return all.stream()
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findById(Integer id) {
        return invoiceDtoRepository.findById(id)
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class));
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return invoiceDtoRepository.findByNumber(number)
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class));
    }
}
