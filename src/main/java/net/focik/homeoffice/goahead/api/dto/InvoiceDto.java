package net.focik.homeoffice.goahead.api.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class InvoiceDto {
    private int idInvoice;
    private int idCustomer;
    private String invoiceNumber;
    private Number amount;
    private LocalDate sellDate;//data sprzedaży
    private LocalDate invoiceDate;//data faktury
    private LocalDate paymentDate;//termin zapłaty
    private int paymentDeadline;
    private PaymentStatusDto paymentStatus;
    private PaymentMethodDto paymentMethod;
    private String paymentTypeView;
    private String otherInfo;
    private String customerName;
    private List<InvoiceItemDto> invoiceItems;

}
