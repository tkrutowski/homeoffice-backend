package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.PurchaseDbDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

interface PurchaseDtoRepository extends JpaRepository<PurchaseDbDto, Integer> {

    List<PurchaseDbDto> findAllByIdUser(Integer idUser);

    List<PurchaseDbDto> findAllByIdCard(Integer idCard);

    List<PurchaseDbDto> findAllByIdFirm(Integer idFirm);

    List<PurchaseDbDto> findAllByIdUserAndPaymentStatus(Integer idUser, PaymentStatus status);

    List<PurchaseDbDto> findAllByIdUserAndPaymentDeadline(Integer idUser, LocalDate deadline);

    @Query(value = "SELECT p FROM PurchaseDbDto p " +
            "LEFT JOIN AppUser u ON p.idUser = u.id " +
            "LEFT JOIN FirmDbDto f ON p.idFirm = f.id " +
            "WHERE (:globalFilter IS NULL OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(p.otherInfo) LIKE LOWER(CONCAT('%', :globalFilter, '%'))) " +
            "AND (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
            "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:purchaseDate IS NULL OR " +
            "(:dateComparisonType = 'EQUALS' AND p.purchaseDate = :purchaseDate) OR " +
            "(:dateComparisonType = 'BEFORE' AND p.purchaseDate < :purchaseDate) OR " +
            "(:dateComparisonType = 'AFTER' AND p.purchaseDate > :purchaseDate)) " +
            "AND (:status IS NULL OR p.paymentStatus = :status)" +
            "AND (:idFirm IS NULL OR p.idFirm = :idFirm)" +
            "AND (:idCard IS NULL OR p.idCard = :idCard)")
    Page<PurchaseDbDto> findPurchaseWithFilters(
            @Param("globalFilter") String globalFilter,
            @Param("username") String username,
            @Param("name") String name,
            @Param("purchaseDate") LocalDate purchaseDate,
            @Param("dateComparisonType") String dateComparisonType,
            @Param("status") PaymentStatus status,
            @Param("idFirm") Integer idFirm,
            @Param("idCard") Integer idCard, Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM PurchaseDbDto p WHERE p.paymentStatus = net.focik.homeoffice.utils.share.PaymentStatus.TO_PAY")
    BigDecimal getTotalSumToPay();
}
