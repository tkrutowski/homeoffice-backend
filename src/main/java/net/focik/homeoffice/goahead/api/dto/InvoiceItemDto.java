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
    private String unit;
    private Number quantity;
    private Double amount;//brutto
}
