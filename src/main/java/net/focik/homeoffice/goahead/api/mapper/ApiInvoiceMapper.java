package net.focik.homeoffice.goahead.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.api.dto.InvoiceDto;
import net.focik.homeoffice.goahead.domain.exception.CustomerNotValidException;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiInvoiceMapper {

    private final ModelMapper mapper;

    public Invoice toDomain(InvoiceDto dto) {
        valid(dto);
        return mapper.map(dto, Invoice.class);
    }

    public InvoiceDto toDto(Invoice invoice) {
        return mapper.map(invoice, InvoiceDto.class);
    }

    private void valid(InvoiceDto dto) {
        if (dto.getCustomer() == null)
            throw new CustomerNotValidException("IdEmployee can't be null.");
//        if (dto.getInvoiceDate().isEmpty())
//            throw new InvoiceNotValidException("Date can't be empty.");
    }
}