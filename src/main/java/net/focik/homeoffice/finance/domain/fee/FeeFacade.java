package net.focik.homeoffice.finance.domain.fee;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.api.mapper.ApiFeeMapper;
import net.focik.homeoffice.finance.domain.fee.port.primary.AddFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.DeleteFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.GetFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.UpdateFeeUseCase;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;

@Log4j2
@RequiredArgsConstructor
@Component
public class FeeFacade implements AddFeeUseCase, GetFeeUseCase, UpdateFeeUseCase, DeleteFeeUseCase {

    private static final int TRANSACTION_CATEGORY_ID_FEE = 3;

    private final FeeService feeService;
    private final UserFacade userFacade;
    private final BankTransactionDtoRepository bankTransactionRepository;
    private final ApiFeeMapper apiFeeMapper;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Fee")
    public Fee addFee(Fee fee) {
        enforceOwnIdUserUnlessPrivilegedToWriteAll(fee);
        return feeService.saveFee(fee);
    }

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "FeeInstallment")
    public FeeInstallment addFeeInstallment(FeeInstallment feeInstallment) {
        return feeService.addFeeInstallment(feeInstallment);
    }

    @Override
    public FeeInstallment getFeeInstallment(int idFeeInstallment) {
        return feeService.getFeeInstallment(idFeeInstallment);
    }

    @Override
    public Fee getFeeById(int idFee, boolean withInstallment) {
        Fee fee = feeService.findFeeById(idFee, withInstallment);
        assertCanReadFee(fee);
        return fee;
    }

    @Override
    public List<Fee> getFeesByUser(int idUser, PaymentStatus paymentStatus, boolean withInstallment) {
        assertCanReadUserData(idUser);
        return feeService.findFeesByUser(idUser, paymentStatus, withInstallment);
    }

    @Override
    public List<Fee> getFeesByUser(int idUser, boolean withInstallment) {
        assertCanReadUserData(idUser);
        return feeService.findFeesByUser(idUser, null, withInstallment);
    }

    @Override
    public List<Fee> getFeesByStatus(PaymentStatus paymentStatus, boolean withInstallment) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // If no authentication context (e.g., scheduled task), return all fees
        if (authentication == null || canReadAllFees(authentication)) {
            return feeService.findFeesByStatus(paymentStatus, withInstallment);
        }

        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        return feeService.findFeesByUser(Math.toIntExact(user.getId()), paymentStatus, withInstallment);
    }

    @Override
    public List<Fee> getFeesByFirm(int idFirm, boolean withInstallment) {
        return feeService.getFeesByFirm(idFirm, withInstallment, null);
    }

    @Override
    public List<FeeInstallment> getFeeInstallments(int idUser, LocalDate date) {
        assertCanReadUserData(idUser);
        return feeService.getFeeInstallments(idUser, date);
    }

    @Override
    public List<FeeInstallment> getFeeInstallments(int idFee) {
        return getFeeById(idFee, true).getInstallments();
    }

    @Override
    public Page<Fee> findFeesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String name, Integer idFirm, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status, Integer idUser) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Zwykly uzytkownik nie moze wymusic filtra idUser na cudze ID - nadpisujemy go wlasnym,
        // niezaleznie od tego, co przyszlo z requestu.
        if (authentication != null && !canReadAllFees(authentication)) {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            idUser = Math.toIntExact(user.getId());
        }

        return feeService.findFeesPageableWithFilters(page, size, sortField, sortDirection, globalFilter, name, idFirm, date, dateComparisonType, amount, amountComparisonType, status, idUser);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do przegladania wszystkich oplat (READ_ALL), a pyta o dane innego uzytkownika niz on sam.
     * Do uzytku w endpointach typu "daj mi oplaty/raty uzytkownika X" (idUser podany wprost).
     */
    private void assertCanReadUserData(int idUser) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || canReadAllFees(authentication)) {
            return;
        }

        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        if (idUser != Math.toIntExact(user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do danych tego użytkownika.");
        }
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Fee")
    public Fee updateFee(Fee fee) {
        Fee existingFee = feeService.findFeeById(fee.getId(), false);
        assertCanWriteFee(existingFee);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !canWriteAllFees(authentication)) {
            // bez uprawnien do zarzadzania cudzymi oplatami nie mozna przepisac oplaty na inna osobe
            fee.setIdUser(existingFee.getIdUser());
        }

        feeService.updateFee(fee);
        return feeService.findFeeById(fee.getId(), true);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "FeeInstallment")
    public FeeInstallment updateFeeInstallment(FeeInstallment feeInstallment) {
        Optional<FeeInstallment> previousInstallment = feeService.getFeeInstallment(feeInstallment.getIdFeeInstallment()) != null ?
                Optional.of(feeService.getFeeInstallment(feeInstallment.getIdFeeInstallment())) : Optional.empty();

        FeeInstallment result = feeService.updateFeeInstallment(feeInstallment);

        if (previousInstallment.isPresent() &&
            previousInstallment.get().getPaymentStatus() != PaymentStatus.PAID &&
            feeInstallment.getPaymentStatus() == PaymentStatus.PAID &&
            feeInstallment.getPaymentDate() != null) {

            // Bezposrednio przez serwis - to wewnetrzny odczyt na potrzeby zapisu transakcji bankowej,
            // a nie odczyt "na zadanie" uzytkownika, wiec nie podlega kontroli wlasnosci z assertCanReadFee.
            Fee fee = feeService.findFeeById(feeInstallment.getIdFee(), false);
            var bankTransaction = apiFeeMapper.toBankTransaction(feeInstallment, fee, TRANSACTION_CATEGORY_ID_FEE);
            bankTransactionRepository.save(bankTransaction);

            log.info("Fee installment paid: feeId={}, installmentId={}, amount={}, date={}, transactionId={}",
                    fee.getId(),
                    feeInstallment.getIdFeeInstallment(),
                    feeInstallment.getInstallmentAmountPaid().getNumber().doubleValue(),
                    feeInstallment.getPaymentDate(),
                    bankTransaction.getId());
        }

        return result;
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Fee")
    public Fee updateFeeStatus(int idFee, PaymentStatus paymentStatus) {
        Fee fee = feeService.findFeeById(idFee, false);
        assertCanWriteFee(fee);
        fee.changeFeeStatus(paymentStatus);

        feeService.updateFee(fee);
        return feeService.findFeeById(idFee, true);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Fee")
    public void deleteFeeById(int idFee) {
        Fee fee = feeService.findFeeById(idFee, false);
        assertCanDeleteFee(fee);

        feeService.deleteFee(idFee);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "FeeInstallment")
    public void deleteFeeInstallmentById(int id) {
        FeeInstallment installment = feeService.getFeeInstallment(id);
        Fee fee = feeService.findFeeById(installment.getIdFee(), false);
        assertCanDeleteFee(fee);

        feeService.deleteFeeInstallment(id);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do przegladania wszystkich oplat (READ_ALL), a podana oplata nie nalezy do niego.
     */
    private void assertCanReadFee(Fee fee) {
        assertCanAccessFee(fee, this::canReadAllFees);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do zarzadzania wszystkimi oplatami (WRITE_ALL), a podana oplata nie nalezy do niego.
     */
    private void assertCanWriteFee(Fee fee) {
        assertCanAccessFee(fee, this::canWriteAllFees);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do usuwania wszystkich oplat (DELETE_ALL), a podana oplata nie nalezy do niego.
     */
    private void assertCanDeleteFee(Fee fee) {
        assertCanAccessFee(fee, this::canDeleteAllFees);
    }

    private void assertCanAccessFee(Fee fee, Predicate<Authentication> hasFullAccess) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Brak kontekstu security (np. scheduler) - traktujemy jak pelny dostep, analogicznie do getFeesByStatus
        if (authentication == null || hasFullAccess.test(authentication)) {
            return;
        }

        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        if (!Objects.equals(fee.getIdUser(), Math.toIntExact(user.getId()))) {
            throw new AccessDeniedException("Brak uprawnień do tej opłaty.");
        }
    }

    /**
     * Wymusza idUser = aktualnie zalogowany uzytkownik, ignorujac wartosc przeslana z klienta,
     * chyba ze uzytkownik ma uprawnienie do zarzadzania cudzymi oplatami.
     */
    private void enforceOwnIdUserUnlessPrivilegedToWriteAll(Fee fee) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && !canWriteAllFees(authentication)) {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            fee.setIdUser(Math.toIntExact(user.getId()));
        }
    }

    private boolean canReadAllFees(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_FEE_READ_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_READ_ALL));
    }

    private boolean canWriteAllFees(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_FEE_WRITE_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_WRITE_ALL));
    }

    private boolean canDeleteAllFees(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_FEE_DELETE_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_DELETE_ALL));
    }

}
