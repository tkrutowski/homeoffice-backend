package net.focik.homeoffice.goahead.api.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.Vat;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CostItemDto {
    private long idCostItem;
    private int idCost;
    private String name;
    private String unit;
    private float quantity;
    private Double amountNet; //netto
    private Double amountVat;
    private Double amountGross; //brutto
    private Vat vat;
}
