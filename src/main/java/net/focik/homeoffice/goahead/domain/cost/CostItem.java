package net.focik.homeoffice.goahead.domain.cost;

import lombok.*;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CostItem {
    private int idCost;
    private long idCostItem;
    private String name;
    private String unit;
    private float quantity;
    private Money amountNet; //netto
    private Money amountVat;
    private Money amountGross; //brutto
    private Vat vat;
}
