package net.focik.homeoffice.finance.domain.loan;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.loan.port.primary.AddLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.DeleteLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.GetLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.UpdateLoanUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;

@AllArgsConstructor
@Component
public class LoanFacade implements AddLoanUseCase, GetLoanUseCase, UpdateLoanUseCase, DeleteLoanUseCase {

    private final LoanService loanService;
    private final UserFacade userFacade;

    @Override
    public Loan addLoan(Loan loan) {
        return loanService.saveLoan(loan);
    }

    @Override
    public LoanInstallment addLoanInstallment(LoanInstallment loanInstallment) {
        return loanService.addLoanInstallment(loanInstallment);
    }

    @Override
    public LoanInstallment getLoanInstallment(int idLoanInstallment) {
        return loanService.getLoanInstallment(idLoanInstallment);
    }

    @Override
    public Loan getLoanById(int idLoan, boolean withInstallment) {
        return loanService.findLoanById(idLoan, withInstallment);
    }

    @Override
    public List<Loan> getLoansByUser(int idUser, PaymentStatus loanStatus, boolean withInstallment) {
        return loanService.findLoansByUser(idUser, loanStatus, withInstallment);
    }

    @Override
    public List<Loan> getLoansByStatus(PaymentStatus loanStatus, boolean withInstallment) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_LOAN_READ_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_READ_ALL));

        if (isAdmin) {
            return loanService.findLoansByStatus(loanStatus, withInstallment);
        } else {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            return loanService.findLoansByUser(Math.toIntExact(user.getId()), loanStatus, withInstallment);
        }
    }

    @Override
    public List<LoanInstallment> getLoanInstallments(int idLoan) {
        return loanService.findLoanById(idLoan, true).getInstallments();
    }

    @Override
    public List<Loan> getLoansByBank(Integer idBank) {
        return loanService.getLoansByBank(idBank);
    }

    @Override
    public Page<Loan> findLoansPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String name, String bankName, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        return loanService.findLoansPageableWithFilters(page, size, sortField, sortDirection, globalFilter, name, bankName, date, dateComparisonType, amount, amountComparisonType, status);
    }

    @Override
    public Loan updateLoan(Loan loan) {
        loanService.updateLoan(loan);
        return loanService.findLoanById(loan.getId(), true);
    }

    @Override
    public LoanInstallment updateLoanInstallment(LoanInstallment loanInstallment) {
        return loanService.updateLoanInstallment(loanInstallment);
    }

    @Override
    public Loan updateLoanStatus(int idLoan, PaymentStatus loanStatus) {
        Loan loan = loanService.findLoanById(idLoan, false);
        loan.changeLoanStatus(loanStatus);

        loanService.updateLoan(loan);
        return loanService.findLoanById(idLoan, true);
    }

    @Override
    public void deleteLoanById(int idLoan) {
        loanService.deleteLoan(idLoan);
    }

    @Override
    public void deleteLoanInstallmentById(int id) {
        loanService.deleteLoanInstallment(id);
    }

}
