package net.focik.homeoffice.finance.domain.fee.port.primary;

import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GetFeeUseCase {

    Fee getFeeById(int idFee, boolean withInstallment);

    List<Fee> getFeesByStatus(PaymentStatus paymentStatus, boolean withInstallment);

    List<Fee> getFeesByFirm(int idFirm, boolean withInstallment);

    List<Fee> getFeesByUser(int idUser, boolean withInstallment);

    List<Fee> getFeesByUser(int idUser, PaymentStatus paymentStatus, boolean withInstallment);

    FeeInstallment getFeeInstallment(int idFeeInstallment);

    List<FeeInstallment> getFeeInstallments(int idUser, LocalDate date);

    List<FeeInstallment> getFeeInstallments(int idFee);

    Page<Fee> findFeesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String name, Integer idFirm, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status, Integer idUser);
}
