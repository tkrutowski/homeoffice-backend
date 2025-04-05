package net.focik.homeoffice.finance.domain.fee;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.domain.fee.port.primary.AddFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.DeleteFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.GetFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.UpdateFeeUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;

@RequiredArgsConstructor
@Component
public class FeeFacade implements AddFeeUseCase, GetFeeUseCase, UpdateFeeUseCase, DeleteFeeUseCase {

    private final FeeService feeService;
    private final UserFacade userFacade;

    @Override
    public Fee addFee(Fee fee) {
        return feeService.saveFee(fee);
    }

    @Override
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
    public Fee updateFee(Fee fee) {
        feeService.updateFee(fee);
        return feeService.findFeeById(fee.getId(), true);
    }

    @Override
    public FeeInstallment updateFeeInstallment(FeeInstallment feeInstallment) {
        return feeService.updateFeeInstallment(feeInstallment);
    }

    @Override
    public Fee updateFeeStatus(int idFee, PaymentStatus paymentStatus) {
        Fee fee = feeService.findFeeById(idFee, false);
        fee.changeFeeStatus(paymentStatus);

        feeService.updateFee(fee);
        return feeService.findFeeById(idFee, true);
    }

    @Override
    public void deleteFeeById(int idFee) {
        feeService.deleteFee(idFee);
    }

    @Override
    public void deleteFeeInstallmentById(int id) {
        feeService.deleteFeeInstallment(id);
    }

}
