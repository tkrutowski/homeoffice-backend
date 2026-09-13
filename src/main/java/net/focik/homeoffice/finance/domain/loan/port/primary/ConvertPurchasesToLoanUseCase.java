package net.focik.homeoffice.finance.domain.loan.port.primary;

import net.focik.homeoffice.finance.domain.loan.Loan;

import java.util.List;

public interface ConvertPurchasesToLoanUseCase {
    Loan convertPurchasesToLoan(List<Integer> purchaseIds, Loan loanData);
}
