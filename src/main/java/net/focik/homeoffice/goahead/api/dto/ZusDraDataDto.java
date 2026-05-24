package net.focik.homeoffice.goahead.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ZusDraDataDto {
    private String period;
    private BigDecimal totalIncome;
    private BigDecimal totalCosts;
}
