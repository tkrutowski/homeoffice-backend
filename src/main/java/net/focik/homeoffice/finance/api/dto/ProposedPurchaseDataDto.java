package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Draft danych zakupu do wypełnienia formularza dodawania zakupu na froncie - pola nazwane
 * tak samo jak w {@link PurchaseDto}, ale wszystkie nullable, bo to tylko propozycja. Celowo bez
 * {@code paymentDeadline} - patrz {@link net.focik.homeoffice.finance.domain.loanproposal.ProposedPurchaseData}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class ProposedPurchaseDataDto {
    private String name;
    private Number amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate purchaseDate;
    private String otherInfo;
}
