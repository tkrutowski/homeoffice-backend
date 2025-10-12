package net.focik.homeoffice.finance.domain.loan.port.secondary;

import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public interface LoanRepository {
    Loan saveLoan(Loan loan);

    LoanInstallment saveLoanInstallment(LoanInstallment loanInstallment);

    List<LoanInstallment> saveLoanInstallment(List<LoanInstallment> loanInstallments);

    Optional<Loan> findLoanById(Integer id);

    Optional<LoanInstallment> findLoanInstallmentById(Integer id);

    List<Loan> findLoanByUserId(Integer idUser);

    List<Loan> findLoanByBankId(Integer idBank);

    List<Loan> findAll();

    List<LoanInstallment> findLoanInstallmentByLoanId(Integer loanId);

    void deleteLoanById(int idLoan);

    void deleteLoanInstallmentById(int idLoanInstallment);

    void deleteLoanInstallmentByIdLoan(int idLoan);

    Page<Loan> findLoanWithFilters(String globalFilter, String name, Integer idBank, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status, Pageable pageable);
}
