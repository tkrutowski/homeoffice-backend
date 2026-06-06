package net.focik.homeoffice.finance.domain.transaction.model;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class TransactionLabel {
    private Integer id;
    private String name;
}
