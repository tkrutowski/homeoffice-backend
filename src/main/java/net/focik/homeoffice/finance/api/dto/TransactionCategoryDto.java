package net.focik.homeoffice.finance.api.dto;

import lombok.*;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategoryType;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class TransactionCategoryDto {
    private int id;
    private String name;
    private TransactionCategoryType type;
    private String color;
    private String icon;
}
