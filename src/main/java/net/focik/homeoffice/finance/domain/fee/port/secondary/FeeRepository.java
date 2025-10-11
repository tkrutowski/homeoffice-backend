package net.focik.homeoffice.finance.domain.fee.port.secondary;

import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    List<Fee> findFeeByFirmId(Integer idFirm);

    List<FeeInstallment> findFeeInstallmentByFeeId(Integer feeId);

    void deleteFeeById(int idFee);

    void deleteFeeInstallmentById(int idFeeInstallment);

    void deleteFeeInstallmentByIdFee(int idFee);

    @Query(value = "SELECT f FROM FeeDbDto f " +
            "LEFT JOIN AppUser u ON f.idUser = u.id " +
            "LEFT JOIN FirmDbDto firm ON f.firm.id = firm.id " +
            "WHERE (:globalFilter IS NULL OR " +
            "LOWER(f.name) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(f.otherInfo) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(firm.name) LIKE LOWER(CONCAT('%', :globalFilter, '%'))) " +
            "AND (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:firmName IS NULL OR LOWER(firm.name) LIKE LOWER(CONCAT('%', :firmName, '%'))) " +
            "AND (:date IS NULL OR " +
            "(:dateComparisonType = 'EQUALS' AND f.date = :date) OR " +
            "(:dateComparisonType = 'BEFORE' AND f.date < :date) OR " +
            "(:dateComparisonType = 'AFTER' AND f.date > :date)) " +
            "AND (:amount IS NULL OR " +
            "(:amountComparisonType = 'EQUALS' AND f.amount = :amount) OR " +
            "(:amountComparisonType = 'LESS_THAN' AND f.amount < :amount) OR " +
            "(:amountComparisonType = 'LESS_THAN_OR_EQUAL' AND f.amount <= :amount) OR " +
            "(:amountComparisonType = 'GREATER_THAN' AND f.amount > :amount) OR " +
            "(:amountComparisonType = 'GREATER_THAN_OR_EQUAL' AND f.amount >= :amount))" +
            "AND (:status IS NULL OR f.feeStatus = :status)")
    Page<Fee> findFeesPageableWithFilters(
            @Param("globalFilter") String globalFilter,
            @Param("name") String name,
            @Param("firmName") String firmName,
            @Param("date") LocalDate date,
            @Param("dateComparisonType") String dateComparisonType,
            @Param("amount") BigDecimal amount,
            @Param("amountComparisonType") String amountComparisonType,
            @Param("status") PaymentStatus status,
            Pageable pageable);
}
