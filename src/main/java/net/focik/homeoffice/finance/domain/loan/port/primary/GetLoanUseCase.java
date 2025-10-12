package net.focik.homeoffice.finance.domain.loan.port.primary;

import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GetLoanUseCase {

    Loan getLoanById(int idLoan, boolean withInstallment);

    List<Loan> getLoansByStatus(PaymentStatus loanStatus, boolean withInstallment);


    List<Loan> getLoansByUser(int idUser, PaymentStatus loanStatus, boolean withInstallment);

    LoanInstallment getLoanInstallment(int idLoanInstallment);

    List<LoanInstallment> getLoanInstallments(int idLoan);

    List<Loan> getLoansByBank(Integer idBank);

    Page<Loan> findLoansPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String name, Integer idBank, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status, Integer idUser);
}
