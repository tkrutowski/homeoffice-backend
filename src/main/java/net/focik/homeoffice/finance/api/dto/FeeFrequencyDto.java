package net.focik.homeoffice.finance.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class FeeFrequencyDto {
    private String name;
    private String viewName;
    private int frequencyNumber;
}
