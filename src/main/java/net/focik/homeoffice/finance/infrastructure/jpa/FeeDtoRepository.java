package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.FeeDbDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


interface FeeDtoRepository extends JpaRepository<FeeDbDto, Integer> {

    List<FeeDbDto> findAllByIdUser(Integer idUser);

    List<FeeDbDto> findAllByFirm_Id(Integer firmId);

    @Query(value = "SELECT f FROM FeeDbDto f " +
            "LEFT JOIN AppUser u ON f.idUser = u.id " +
            "LEFT JOIN FirmDbDto firm ON f.firm.id = firm.id " +
            "WHERE (:globalFilter IS NULL OR " +
            "LOWER(f.name) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(f.otherInfo) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(firm.name) LIKE LOWER(CONCAT('%', :globalFilter, '%'))) " +
            "AND (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
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
            "AND (:idFirm IS NULL OR f.firm.id = :idFirm)" +
            "AND (:idUser IS NULL OR f.idUser = :idUser)" +
            "AND (:status IS NULL OR f.feeStatus = :status)")
    Page<FeeDbDto> findFeesPageableWithFilters(
            @Param("globalFilter") String globalFilter,
            @Param("name") String name,
            @Param("idFirm") Integer idFirm,
            @Param("date") LocalDate date,
            @Param("dateComparisonType") String dateComparisonType,
            @Param("amount") BigDecimal amount,
            @Param("amountComparisonType") String amountComparisonType,
            @Param("status") PaymentStatus status,
            @Param("idUser") Integer idUser, Pageable pageable);
}
