package net.focik.homeoffice.finance.domain.payment;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeFacade;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanFacade;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Component
public class PaymentFacade {

    private final FeeFacade feeFacade;
    private final LoanFacade loanFacade;
    private final UserFacade userFacade;
    private final PaymentService paymentService;

    /**
     * @param date data (year)
     * @return map user, list of payments
     */
    public Map<Integer, List<Payment>> getPaymentsByDate(LocalDate date, PaymentStatus paymentStatus) {
        Map<Integer, List<Payment>> resultMap;

        boolean hasAdminAccess = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN") ||
                        (grantedAuthority.getAuthority().equals("FINANCE_PAYMENT_READ_ALL")));

        if (hasAdminAccess) {
            List<Fee> fees = feeFacade.getFeesByStatus(paymentStatus, true);
            List<Loan> loans = loanFacade.getLoansByStatus(paymentStatus, true);

            resultMap = paymentService.getFinancialTransactionMap(date, fees, loans);

        } else {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            List<Fee> feesByUser = feeFacade.getFeesByUser(Math.toIntExact(user.getId()), paymentStatus, true);
            List<Loan> loansByUser = loanFacade.getLoansByUser(Math.toIntExact(user.getId()), paymentStatus, true);

            resultMap = paymentService.getFinancialTransactionMap(date, feesByUser, loansByUser);
        }
        return resultMap;
    }
}