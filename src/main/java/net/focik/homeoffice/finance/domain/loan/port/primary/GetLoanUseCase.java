package net.focik.homeoffice.finance.domain.loan.port.primary;

import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.utils.share.PaymentStatus;

import java.util.List;

public interface GetLoanUseCase {

    Loan getLoanById(int idLoan, boolean withInstallment);

    List<Loan> getLoansByStatus(PaymentStatus loanStatus, boolean withInstallment);


    List<Loan> getLoansByUser(int idUser, PaymentStatus loanStatus, boolean withInstallment);

    LoanInstallment getLoanInstallment(int idLoanInstallment);

    List<LoanInstallment> getLoanInstallments(int idLoan);

    List<Loan> getLoansByBank(Integer idBank);
}
