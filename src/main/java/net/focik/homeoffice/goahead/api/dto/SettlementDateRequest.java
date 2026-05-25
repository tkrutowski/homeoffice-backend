package net.focik.homeoffice.goahead.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SettlementDateRequest {
    private LocalDate settlementDate;
}
