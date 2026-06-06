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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        return feeService.findFeeById(idFee, withInstallment);
    }

    @Override
    public List<Fee> getFeesByUser(int idUser, PaymentStatus paymentStatus, boolean withInstallment) {
        return feeService.findFeesByUser(idUser, paymentStatus, withInstallment);
    }

    @Override
    public List<Fee> getFeesByUser(int idUser, boolean withInstallment) {
        return feeService.findFeesByUser(idUser, null, withInstallment);
    }

    @Override
    public List<Fee> getFeesByStatus(PaymentStatus paymentStatus, boolean withInstallment) {

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_FEE_READ_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PAYMENT_READ_ALL));

        if (isAdmin) {
            return feeService.findFeesByStatus(paymentStatus, withInstallment);
        } else {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            return feeService.findFeesByUser(Math.toIntExact(user.getId()), paymentStatus, withInstallment);
        }
    }

    @Override
    public List<Fee> getFeesByFirm(int idFirm, boolean withInstallment) {
        return feeService.getFeesByFirm(idFirm, withInstallment, null);
    }

    @Override
    public List<FeeInstallment> getFeeInstallments(int idUser, LocalDate date) {
        return feeService.getFeeInstallments(idUser, date);
    }

    @Override
    public List<FeeInstallment> getFeeInstallments(int idFee) {
        return feeService.findFeeById(idFee, true).getInstallments();
    }

    @Override
    public Page<Fee> findFeesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String name, Integer idFirm, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status, Integer idUser) {
        return feeService.findFeesPageableWithFilters(page, size, sortField, sortDirection, globalFilter, name, idFirm, date, dateComparisonType, amount, amountComparisonType, status, idUser);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Fee")
    public Fee updateFee(Fee fee) {
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

            Fee fee = this.getFeeById(feeInstallment.getIdFee(), false);
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
        fee.changeFeeStatus(paymentStatus);

        feeService.updateFee(fee);
        return feeService.findFeeById(idFee, true);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Fee")
    public void deleteFeeById(int idFee) {
        feeService.deleteFee(idFee);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "FeeInstallment")
    public void deleteFeeInstallmentById(int id) {
        feeService.deleteFeeInstallment(id);
    }

}
