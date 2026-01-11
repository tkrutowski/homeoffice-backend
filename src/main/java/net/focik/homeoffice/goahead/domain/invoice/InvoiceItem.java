package net.focik.homeoffice.goahead.domain.invoice;

import lombok.*;
import net.focik.homeoffice.utils.share.Vat;
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
    private Vat vat;
}
