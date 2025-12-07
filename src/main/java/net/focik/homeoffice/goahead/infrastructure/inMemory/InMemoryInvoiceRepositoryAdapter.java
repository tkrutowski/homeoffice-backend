package net.focik.homeoffice.goahead.infrastructure.inMemory;

import lombok.extern.java.Log;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.port.secondary.InvoiceRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Profile({"test"})
@Log
public class InMemoryInvoiceRepositoryAdapter implements InvoiceRepository {

    private final ModelMapper mapper = new ModelMapper();

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceDbDto invoiceDbDto = mapper.map(invoice, InvoiceDbDto.class);
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


        log.info("Succssec idInvoice = " + idInvoice);
        InvoiceDbDto dbDto = DataBaseInvoice.getInvoiceDbDtoHashMap().get(idInvoice);
        return mapper.map(dbDto, Invoice.class);
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
                .map(customerDbDto -> mapper.map(customerDbDto, Invoice.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findById(Integer id) {
        Optional<Invoice> invoiceOptional = Optional.ofNullable(DataBaseInvoice.getInvoiceDbDtoHashMap().get(id))
                .map(dbDto -> mapper.map(dbDto, Invoice.class));

        return invoiceOptional;
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return DataBaseInvoice.getInvoiceDbDtoHashMap()
                .values()
                .stream()
                .filter(dto -> dto.getNumber().equals(number))
                .map(invoiceDbDto -> mapper.map(invoiceDbDto, Invoice.class))
                .findFirst();
    }

    @Override
    public Page<Invoice> findAll(Pageable pageable, String globalFilter, Integer idCustomer, LocalDate sellDate, String sellDateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        return null;
    }

    @Override
    public List<Invoice> findLastInvoiceNumberByYear(Integer year) {
        return List.of();
    }
}
