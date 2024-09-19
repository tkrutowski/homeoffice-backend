package net.focik.homeoffice.goahead.api.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class InvoiceItemDto {
    private long id;
    private int idInvoice;
    private String name;
    private String jm;
    private Number quantity;
    private Number amount;//brutto
    private Number amountSum;//brutto
}
