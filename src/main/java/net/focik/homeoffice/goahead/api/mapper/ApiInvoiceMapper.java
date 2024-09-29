package net.focik.homeoffice.goahead.api.mapper;

import net.focik.homeoffice.goahead.api.dto.InvoiceDto;
import net.focik.homeoffice.goahead.api.dto.InvoiceItemDto;
import net.focik.homeoffice.goahead.api.dto.PaymentMethodDto;
import net.focik.homeoffice.goahead.api.dto.PaymentStatusDto;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.exception.CustomerNotValidException;
import net.focik.homeoffice.goahead.domain.invoice.Invoice;
import net.focik.homeoffice.goahead.domain.invoice.InvoiceItem;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentMethod;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Component
public class ApiInvoiceMapper {

    public Invoice toDomain(InvoiceDto dto) {
        valid(dto);
        return Invoice.builder()
                .idInvoice(dto.getIdInvoice())
                .customer(Customer.builder().id(dto.getIdCustomer()).build())
                .invoiceNumber(dto.getInvoiceNumber())
                .invoiceDate(dto.getInvoiceDate())
                .sellDate(dto.getSellDate())
                .invoiceDate(dto.getInvoiceDate())
//                .amount(Money.of(BigDecimal.valueOf(Double.parseDouble(dto.getAmount())),"PLN"))
                .paymentDate(dto.getInvoiceDate().plusDays(dto.getPaymentDeadline()))
                .paymentStatus(PaymentStatus.valueOf(dto.getPaymentStatus().getName()))
                .paymentMethod(PaymentMethod.valueOf(dto.getPaymentMethod().getName()))
                .otherInfo(dto.getOtherInfo())
                .invoiceItems(mapToList(dto.getInvoiceItems()))
                .build();
    }

    public InvoiceDto toDto(Invoice invoice) {
        return InvoiceDto.builder()
                .idInvoice(invoice.getIdInvoice())
                .idCustomer(invoice.getCustomer().getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getAmount().getNumber().doubleValue())
                .sellDate(invoice.getSellDate())
                .invoiceDate(invoice.getInvoiceDate())
                .paymentDeadline(Period.between(invoice.getInvoiceDate(),invoice.getPaymentDate()).getDays())
                .paymentDate(invoice.getPaymentDate())
                .paymentStatus(new PaymentStatusDto(invoice.getPaymentStatus().toString(), invoice.getPaymentStatus().getViewValue()))
                .paymentMethod(new PaymentMethodDto(invoice.getPaymentMethod().toString(), invoice.getPaymentMethod().getViewValue()))
                .otherInfo(invoice.getOtherInfo())
                .invoiceItems(mapToDtoList(invoice.getInvoiceItems()))
                .build();
    }

    public InvoiceItemDto toDto(InvoiceItem item) {
        return InvoiceItemDto.builder()
                .id(item.getIdInvoiceItem())
                .idInvoice(item.getIdInvoice())
                .name(item.getName())
                .jm(item.getUnit())
                .quantity(item.getQuantity())
//                .amount(MoneyUtils.mapMoneyToString(item.getAmount()))
                .amount(item.getAmount().getNumber().doubleValue())
//                .amountSum(MoneyUtils.mapMoneyToString(item.getAmount().multiply(item.getQuantity())))
                .amountSum(item.getAmount().multiply(item.getQuantity()).getNumber().doubleValue())
                .build();
    }

    public InvoiceItem toDomain(InvoiceItemDto dto) {
        return InvoiceItem.builder()
                .idInvoiceItem(dto.getId())
                .idInvoice(dto.getIdInvoice())
                .name(dto.getName())
                .unit(dto.getJm())
                .quantity(dto.getQuantity().floatValue())
                .amount(Money.of(BigDecimal.valueOf(dto.getAmount().doubleValue()), "PLN"))
                .build();
    }

    private List<InvoiceItemDto> mapToDtoList(List<InvoiceItem> input){
        List<InvoiceItemDto> output = new ArrayList<>();
        if(input != null){
            input.forEach(invoiceItem -> output.add(toDto(invoiceItem)));
        }
        return output;
    }

    private List<InvoiceItem> mapToList(List<InvoiceItemDto> input){
        List<InvoiceItem> output = new ArrayList<>();
        if(input != null){
            input.forEach(invoiceItem -> output.add(toDomain(invoiceItem)));
        }
        return output;
    }

    private void valid(InvoiceDto dto) {
        if (dto.getIdCustomer() == 0)
            throw new CustomerNotValidException("IdEmployee can't be null.");
//        if (dto.getInvoiceDate().isEmpty())
//            throw new InvoiceNotValidException("Date can't be empty.");
    }
}