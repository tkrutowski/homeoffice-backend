package net.focik.homeoffice.finance.api.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class TransactionLabelDto {
    private int id;
    private String name;
}
