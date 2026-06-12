package net.focik.homeoffice.finance.api.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class ImportErrorDto {
    private int rowNumber;
    private String errorMessage;
    private String csvLineContent;
}
