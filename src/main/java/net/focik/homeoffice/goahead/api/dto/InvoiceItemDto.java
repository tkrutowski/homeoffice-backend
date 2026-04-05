package net.focik.homeoffice.goahead.api.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.Vat;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class InvoiceItemDto {
    private long idInvoiceItem;
    private int idInvoice;
    private String name;
    private String unit;
    private Number quantity;
    private Double amount;//brutto
    private Vat vat;
}
