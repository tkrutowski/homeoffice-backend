package net.focik.homeoffice.finance.domain.transaction.model;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class TransactionImportError {
    private int rowNumber;
    private String errorMessage;
    private String csvLineContent;
}
