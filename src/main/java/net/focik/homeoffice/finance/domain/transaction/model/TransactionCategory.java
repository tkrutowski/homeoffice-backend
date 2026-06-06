package net.focik.homeoffice.finance.domain.transaction.model;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class TransactionCategory {
    private Integer id;
    private String name;
    private TransactionCategoryType type;
    private String color;
    private String icon;
}
