package net.focik.homeoffice.goahead.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;

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
    private CustomerDto customer;
    private String invoiceNumber;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate sellDate;//data sprzedaży
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate invoiceDate;//data faktury
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate paymentDate;//termin zapłaty
    private int paymentDeadline;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String otherInfo;
    private List<InvoiceItemDto> invoiceItems;

}
