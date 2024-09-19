package net.focik.homeoffice.finance.domain.fee.port.secondary;

import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public interface FeeRepository {
    Fee saveFee(Fee loan);

    FeeInstallment saveFeeInstallment(FeeInstallment feeInstallment);
    List<FeeInstallment> saveFeeInstallment(List<FeeInstallment> feeInstallment);

    Optional<Fee> findFeeById(Integer id);

    Optional<FeeInstallment> findFeeInstallmentById(Integer id);

    List<Fee> findFeeByUserId(Integer idUser);

    List<Fee> findAll();

//    List<FeeInstallment> findFeeInstallmentByDate(LocalDate date);

    List<FeeInstallment> findFeeInstallmentByFeeId(Integer feeId);

    void deleteFeeById(int idFee);

    void deleteFeeInstallmentById(int idFeeInstallment);

    void deleteFeeInstallmentByIdFee(int idFee);
}
