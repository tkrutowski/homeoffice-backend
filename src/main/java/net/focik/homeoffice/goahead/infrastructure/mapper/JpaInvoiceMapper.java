package net.focik.homeoffice.goahead.infrastructure.mapper;

import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.InvoiceItem;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.spi.MoneyUtils;
import org.springframework.stereotype.Component;

@Component
public class JpaInvoiceMapper {

    public InvoiceDbDto toDto(Invoice i) {
        return InvoiceDbDto.builder()
                .idInvoice(i.getIdInvoice())
                .idCustomer(i.getCustomer().getId())
                .number(i.getInvoiceNumber())
                .paymentMethod(i.getPaymentMethod())
                .sellDate(i.getSellDate())
                .invoiceDate(i.getInvoiceDate())
                .amount(MoneyUtils.getBigDecimal(i.getAmount().getNumber()))
                .paymentStatus(i.getPaymentStatus())
                .paymentDate(i.getPaymentDate())
                .otherInfo(i.getOtherInfo())
                .build();
    }

    public Invoice toDomain(InvoiceDbDto dto) {
        return Invoice.builder()
                .idInvoice(dto.getIdInvoice())
                .customer(Customer.builder().id(dto.getIdCustomer()).build())
                .paymentMethod(dto.getPaymentMethod())
                .invoiceDate(dto.getInvoiceDate())
                .sellDate(dto.getSellDate())
                .amount(Money.of(dto.getAmount(),"PLN"))
                .paymentStatus(dto.getPaymentStatus())
                .paymentDate(dto.getPaymentDate())
                .invoiceNumber(dto.getNumber())
                .otherInfo(dto.getOtherInfo())
                .build();
    }

    public InvoiceItemDbDto toDto(InvoiceItem i) {
        return InvoiceItemDbDto.builder()
                .idInvoice(i.getIdInvoice())
                .idInvoiceItem(i.getIdInvoiceItem())
                .name(i.getName())
                .pkwiu(i.getPkwiu())
                .unit(i.getUnit())
                .quantity(i.getQuantity())
                .amount(MoneyUtils.getBigDecimal(i.getAmount().getNumber()))
                .build();
    }

    public InvoiceItem toDomain(InvoiceItemDbDto dto) {
        return InvoiceItem.builder()
                .idInvoice(dto.getIdInvoice())
                .idInvoiceItem(dto.getIdInvoiceItem())
                .name(dto.getName())
                .pkwiu(dto.getPkwiu())
                .unit(dto.getUnit())
                .quantity(dto.getQuantity())
                .amount(Money.of(dto.getAmount(),"PLN"))
                .build();
    }
}