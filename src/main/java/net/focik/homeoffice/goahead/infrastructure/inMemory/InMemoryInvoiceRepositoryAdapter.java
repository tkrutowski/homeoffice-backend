package net.focik.homeoffice.goahead.infrastructure.inMemory;

import lombok.extern.java.Log;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.InvoiceItem;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;
import net.focik.homeoffice.goahead.infrastructure.mapper.JpaInvoiceMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Profile({"test"})
@Log
public class InMemoryInvoiceRepositoryAdapter implements InvoiceRepository {

    private final JpaInvoiceMapper mapper = new JpaInvoiceMapper();

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceDbDto invoiceDbDto = mapper.toDto(invoice);
        log.info("Try add into inMemoryDb invoice: " + invoiceDbDto.toString());
        if (invoiceDbDto == null)
            throw new NullPointerException("Advance cannot be null");
        Integer idInvoice = DataBaseInvoice.getInvoiceDbDtoHashMap()
                .keySet()
                .stream()
                .reduce(Integer::max)
                .orElse(Integer.valueOf("0")) + 1;

        if (invoiceDbDto.getIdInvoice() == 0) {
            invoiceDbDto.setIdInvoice(idInvoice);
        }
        DataBaseInvoice.getInvoiceDbDtoHashMap().put(idInvoice, invoiceDbDto);

        Long idInvoiceItem = DataBaseInvoiceItem.getInvoiceItemDbDtoHashMap()
                .keySet()
                .stream()
                .reduce(Long::max)
                .orElse(Long.valueOf("0")) + 1;

        invoice.getInvoiceItems()
                .forEach(invoiceItem -> invoiceItem.setIdInvoice(idInvoice));


        for (InvoiceItem item: invoice.getInvoiceItems()){
            item.setIdInvoiceItem(idInvoiceItem);
            InvoiceItemDbDto invoiceItemDbDto = mapper.toDto(item);
            DataBaseInvoiceItem.getInvoiceItemDbDtoHashMap().put(idInvoiceItem, invoiceItemDbDto);
            idInvoiceItem++;
        }

        log.info("Succssec idInvoice = " + idInvoice);
        InvoiceDbDto dbDto = DataBaseInvoice.getInvoiceDbDtoHashMap().get(idInvoice);
        return mapper.toDomain(dbDto);
    }

    @Override
    public void deleteInvoice(Integer id) {

    }

    public void delete(Integer id) {
        DataBaseInvoice.getInvoiceDbDtoHashMap().remove(id);
    }

    public List<Invoice> findAll() {
        return DataBaseInvoice.getInvoiceDbDtoHashMap()
                .values()
                .stream()
                .map(customerDbDto -> mapper.toDomain(customerDbDto))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findById(Integer id) {
        Optional<Invoice> invoiceOptional = Optional.ofNullable(DataBaseInvoice.getInvoiceDbDtoHashMap().get(id))
                .map(dbDto -> mapper.toDomain(dbDto));

        if (invoiceOptional.isPresent()){

                invoiceOptional.get().setInvoiceItems(DataBaseInvoiceItem.getInvoiceItemDbDtoHashMap().values()
                        .stream().filter(invoiceItemDbDto -> invoiceItemDbDto.getIdInvoice() == id)
                        .map(invoiceItemDbDto -> mapper.toDomain(invoiceItemDbDto))
                        .collect(Collectors.toList()));
        }
        return invoiceOptional;
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return DataBaseInvoice.getInvoiceDbDtoHashMap()
                .values()
                .stream()
                .filter(dto -> dto.getNumber().equals(number))
                .map(mapper::toDomain)
                .findFirst();
    }

    @Override
    public List<InvoiceItem> findByInvoiceId(Integer idInvoice) {
        return DataBaseInvoiceItem.getInvoiceItemDbDtoHashMap()
                .values()
                .stream()
                .filter(dto -> dto.getIdInvoice() == idInvoice)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void removeInvoiceItem(Long id) {

    }

    @Override
    public void deleteAllInvoiceItemsByInvoiceId(Integer id) {

    }

}
