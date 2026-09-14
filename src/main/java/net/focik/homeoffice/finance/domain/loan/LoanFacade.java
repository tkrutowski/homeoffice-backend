package net.focik.homeoffice.finance.domain.loan;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.api.mapper.ApiLoanMapper;
import net.focik.homeoffice.finance.domain.loan.port.primary.AddLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.DeleteLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.GetLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.UpdateLoanUseCase;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;

@Log4j2
@AllArgsConstructor
@Component
public class LoanFacade implements AddLoanUseCase, GetLoanUseCase, UpdateLoanUseCase, DeleteLoanUseCase {

    private static final int TRANSACTION_CATEGORY_ID_LOAN = 7;
    private static final int FIRM_ID_LOAN_PAYMENT = 38;

    private final LoanService loanService;
    private final UserFacade userFacade;
    private final BankTransactionDtoRepository bankTransactionRepository;
    private final ApiLoanMapper apiLoanMapper;
    private final GetPurchaseUseCase getPurchaseUseCase;
    private final UpdatePurchaseUseCase updatePurchaseUseCase;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Loan")
    public Loan addLoan(Loan loan) {
        enforceOwnIdUserUnlessPrivilegedToWriteAll(loan);
        return loanService.saveLoan(loan);
    }

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "LoanInstallment")
    public LoanInstallment addLoanInstallment(LoanInstallment loanInstallment) {
        return loanService.addLoanInstallment(loanInstallment);
    }

    @Override
    public LoanInstallment getLoanInstallment(int idLoanInstallment) {
        return loanService.getLoanInstallment(idLoanInstallment);
    }

    @Override
    public Loan getLoanById(int idLoan, boolean withInstallment) {
        Loan loan = loanService.findLoanById(idLoan, withInstallment);
        assertCanReadLoan(loan);
        return loan;
    }

    @Override
    public List<Loan> getLoansByUser(int idUser, PaymentStatus loanStatus, boolean withInstallment) {
        return loanService.findLoansByUser(idUser, loanStatus, withInstallment);
    }

    @Override
    public List<Loan> getLoansByStatus(PaymentStatus loanStatus, boolean withInstallment) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // If no authentication context (e.g., scheduled task), return all loans
        if (authentication == null || canReadAllLoans(authentication)) {
            return loanService.findLoansByStatus(loanStatus, withInstallment);
        }

        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        return loanService.findLoansByUser(Math.toIntExact(user.getId()), loanStatus, withInstallment);
    }

    @Override
    public List<LoanInstallment> getLoanInstallments(int idLoan) {
        return getLoanById(idLoan, true).getInstallments();
    }

    @Override
    public List<Loan> getLoansByBank(Integer idBank) {
        return loanService.getLoansByBank(idBank);
    }

    @Override
    public Page<Loan> findLoansPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String name, Integer idBank, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status, Integer idUser) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Zwykły użytkownik nie może wymusić filtra idUser na cudze ID - nadpisujemy go własnym,
        // niezależnie od tego, co przyszło z requestu.
        if (authentication != null && !canReadAllLoans(authentication)) {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            idUser = Math.toIntExact(user.getId());
        }

        return loanService.findLoansPageableWithFilters(page, size, sortField, sortDirection, globalFilter, name, idBank, date, dateComparisonType, amount, amountComparisonType, status, idUser);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jeśli aktualnie zalogowany użytkownik nie ma uprawnień
     * do przeglądania wszystkich kredytów (READ_ALL), a podany kredyt nie należy do niego.
     * Do użytku przy operacjach odczytu.
     */
    private void assertCanReadLoan(Loan loan) {
        assertCanAccessLoan(loan, this::canReadAllLoans);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jeśli aktualnie zalogowany użytkownik nie ma uprawnień
     * do zarządzania wszystkimi kredytami (WRITE_ALL), a podany kredyt nie należy do niego.
     * Do użytku przy operacjach zapisu (edycja, zmiana statusu) - celowo osobne uprawnienie od
     * odczytu, bo ktoś może mieć prawo widzieć wszystkie kredyty (np. rola raportowa), a nie mieć
     * prawa ich edytować, i odwrotnie.
     */
    private void assertCanWriteLoan(Loan loan) {
        assertCanAccessLoan(loan, this::canWriteAllLoans);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jeśli aktualnie zalogowany użytkownik nie ma uprawnień
     * do usuwania wszystkich kredytów (DELETE_ALL), a podany kredyt nie należy do niego.
     * Osobne uprawnienie od READ_ALL/WRITE_ALL z tego samego powodu - ktoś może mieć prawo
     * edytować kredyty, a nie mieć prawa ich usuwać, i odwrotnie.
     */
    private void assertCanDeleteLoan(Loan loan) {
        assertCanAccessLoan(loan, this::canDeleteAllLoans);
    }

    private void assertCanAccessLoan(Loan loan, Predicate<Authentication> hasFullAccess) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Brak kontekstu security (np. scheduler) - traktujemy jak pełny dostęp, analogicznie do getLoansByStatus
        if (authentication == null || hasFullAccess.test(authentication)) {
            return;
        }

        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        if (loan.getIdUser() != Math.toIntExact(user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do tego kredytu.");
        }
    }

    /**
     * Wymusza idUser = aktualnie zalogowany użytkownik, ignorując wartość przesłaną z klienta,
     * chyba że użytkownik ma uprawnienie do zarządzania (WRITE_ALL) cudzymi kredytami. Bez tego
     * dowolny ROLE_FINANCE mógłby dodać kredyt na konto innej osoby, po prostu podmieniając idUser
     * w requeście.
     */
    private void enforceOwnIdUserUnlessPrivilegedToWriteAll(Loan loan) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && !canWriteAllLoans(authentication)) {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            loan.setIdUser(Math.toIntExact(user.getId()));
        }
    }

    private boolean canReadAllLoans(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_LOAN_READ_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_READ_ALL));
    }

    private boolean canWriteAllLoans(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_LOAN_WRITE_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_WRITE_ALL));
    }

    private boolean canDeleteAllLoans(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_LOAN_DELETE_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_DELETE_ALL));
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Loan")
    public Loan updateLoan(Loan loan) {
        Loan existingLoan = loanService.findLoanById(loan.getId(), false);
        assertCanWriteLoan(existingLoan);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !canWriteAllLoans(authentication)) {
            // bez uprawnien do zarzadzania cudzymi kredytami nie mozna przepisac kredytu na inna osobe
            loan.setIdUser(existingLoan.getIdUser());
        }

        loanService.updateLoan(loan);
        return loanService.findLoanById(loan.getId(), true);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "LoanInstallment")
    public LoanInstallment updateLoanInstallment(LoanInstallment loanInstallment) {
        Optional<LoanInstallment> previousInstallment = loanService.getLoanInstallment(loanInstallment.getIdLoanInstallment()) != null ?
                Optional.of(loanService.getLoanInstallment(loanInstallment.getIdLoanInstallment())) : Optional.empty();

        LoanInstallment result = loanService.updateLoanInstallment(loanInstallment);

        if (previousInstallment.isPresent() &&
            previousInstallment.get().getPaymentStatus() != PaymentStatus.PAID &&
            loanInstallment.getPaymentStatus() == PaymentStatus.PAID &&
            loanInstallment.getPaymentDate() != null) {

            // Bezpośrednio przez serwis - to wewnętrzny odczyt na potrzeby zapisu transakcji bankowej,
            // a nie odczyt "na żądanie" użytkownika, więc nie podlega kontroli własności z assertCanAccessLoan.
            Loan loan = loanService.findLoanById(loanInstallment.getIdLoan(), false);
            var bankTransaction = apiLoanMapper.toBankTransaction(loanInstallment, loan, TRANSACTION_CATEGORY_ID_LOAN, FIRM_ID_LOAN_PAYMENT);
            bankTransactionRepository.save(bankTransaction);

            log.info("Loan installment paid: loanId={}, installmentId={}, amount={}, date={}, transactionId={}",
                    loan.getId(),
                    loanInstallment.getIdLoanInstallment(),
                    loanInstallment.getInstallmentAmountPaid().getNumber().doubleValue(),
                    loanInstallment.getPaymentDate(),
                    bankTransaction.getId());
        }

        return result;
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Loan")
    public Loan updateLoanStatus(int idLoan, PaymentStatus loanStatus) {
        Loan loan = loanService.findLoanById(idLoan, false);
        assertCanWriteLoan(loan);
        loan.changeLoanStatus(loanStatus);

        loanService.updateLoan(loan);
        return loanService.findLoanById(idLoan, true);
    }

    @Override
    @Transactional
    @AuditLog(action = AuditAction.DELETE, entityType = "Loan")
    public void deleteLoanById(int idLoan) {
        Loan loan = loanService.findLoanById(idLoan, false);
        assertCanDeleteLoan(loan);

        unlinkConvertedPurchases(idLoan);
        loanService.deleteLoan(idLoan);
    }

    /**
     * Odwraca skutek {@code ConvertPurchasesToLoanUseCase} - zakupy wchlonięte przez ten kredyt
     * (zob. {@code Purchase.idLoan}) wracają do statusu {@code TO_PAY} jako samodzielne zakupy,
     * zamiast zostać osierocone (z FK na usuniety Loan) po skasowaniu kredytu.
     */
    private void unlinkConvertedPurchases(int idLoan) {
        List<Purchase> linkedPurchases = getPurchaseUseCase.findByLoan(idLoan);

        linkedPurchases.forEach(purchase -> {
            purchase.setIdLoan(null);
            purchase.changePaymentStatus(PaymentStatus.TO_PAY);
            updatePurchaseUseCase.updatePurchase(purchase);
        });

        if (!linkedPurchases.isEmpty()) {
            log.info("Unlinked {} purchase(s) from loan id={} before deletion", linkedPurchases.size(), idLoan);
        }
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "LoanInstallment")
    public void deleteLoanInstallmentById(int id) {
        LoanInstallment installment = loanService.getLoanInstallment(id);
        Loan loan = loanService.findLoanById(installment.getIdLoan(), false);
        assertCanDeleteLoan(loan);

        loanService.deleteLoanInstallment(id);
    }

}
