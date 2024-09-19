package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.InvoiceItem;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;
import net.focik.homeoffice.goahead.infrastructure.mapper.JpaInvoiceMapper;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final InvoiceDtoRepository invoiceDtoRepository;
    private final InvoiceItemDtoRepository invoiceItemDtoRepository;
    private final JpaInvoiceMapper mapper;

    @Override
    @Transactional
    public Invoice save(Invoice invoice) {
        List<InvoiceItem> invoiceItems = invoice.getInvoiceItems();
        InvoiceDbDto dbDto = mapper.toDto(invoice);
        InvoiceDbDto saved = invoiceDtoRepository.save(dbDto);
        invoiceItems.forEach(item -> item.setIdInvoice(saved.getIdInvoice()));

        List<InvoiceItemDbDto> dtoList = invoiceItems.stream()
                .map(invoiceItem -> mapper.toDto(invoiceItem))
                .collect(Collectors.toList());
        invoiceItemDtoRepository.saveAll(dtoList);

        return mapper.toDomain(saved);
    }

    @Override
    public void deleteInvoice(Integer id) {
        invoiceDtoRepository.deleteById(id);
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findById(Integer id) {
        return invoiceDtoRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return invoiceDtoRepository.findByNumber(number)
                .map(mapper::toDomain);
    }


    @Override
    public List<InvoiceItem> findByInvoiceId(Integer idInvoice) {
        return invoiceItemDtoRepository.findAllByIdInvoice(idInvoice).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void removeInvoiceItem(Long id) {
        invoiceItemDtoRepository.deleteById(id);
    }

    @Override
    public void deleteAllInvoiceItemsByInvoiceId(Integer id) {
        invoiceItemDtoRepository.deleteAllByIdInvoice(id);
    }

}
