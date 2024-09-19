package net.focik.homeoffice.goahead.domain.invoice;

import lombok.*;
import org.javamoney.moneta.Money;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class InvoiceItem {
    private int idInvoice;
    private long idInvoiceItem;
    private String name;
    private String pkwiu;
    private String unit;
    private float quantity;
    private Money amount;//brutto
}
