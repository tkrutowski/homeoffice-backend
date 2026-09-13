package net.focik.homeoffice.finance.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class ConvertPurchasesToLoanRequestDto {
    private List<Integer> purchaseIds;
    private LoanDto loan;
}
