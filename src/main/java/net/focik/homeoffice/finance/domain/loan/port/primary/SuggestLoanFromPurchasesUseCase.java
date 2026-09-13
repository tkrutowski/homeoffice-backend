package net.focik.homeoffice.finance.domain.loan.port.primary;

import net.focik.homeoffice.finance.domain.loan.LoanFromPurchasesDraft;

import java.util.List;

public interface SuggestLoanFromPurchasesUseCase {
    LoanFromPurchasesDraft suggestLoanFromPurchases(List<Integer> purchaseIds);
}
