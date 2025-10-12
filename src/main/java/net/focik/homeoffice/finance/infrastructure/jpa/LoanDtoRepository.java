package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.LoanDbDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


interface LoanDtoRepository extends JpaRepository<LoanDbDto, Integer> {

    List<LoanDbDto> findAllByIdUser(Integer idUser);

    List<LoanDbDto> findAllByBank_Id(Integer bankId);

    @Query(value = "SELECT l FROM LoanDbDto l " +
            "LEFT JOIN AppUser u ON l.idUser = u.id " +
            "LEFT JOIN BankDbDto b ON l.bank.id = b.id " +
            "WHERE (:globalFilter IS NULL OR " +
            "LOWER(l.name) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(l.otherInfo) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :globalFilter, '%'))) " +
            "AND (:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:date IS NULL OR " +
            "(:dateComparisonType = 'EQUALS' AND l.date = :date) OR " +
            "(:dateComparisonType = 'BEFORE' AND l.date < :date) OR " +
            "(:dateComparisonType = 'AFTER' AND l.date > :date)) " +
            "AND (:amount IS NULL OR " +
            "(:amountComparisonType = 'EQUALS' AND l.amount = :amount) OR " +
            "(:amountComparisonType = 'LESS_THAN' AND l.amount < :amount) OR " +
            "(:amountComparisonType = 'LESS_THAN_OR_EQUAL' AND l.amount <= :amount) OR " +
            "(:amountComparisonType = 'GREATER_THAN' AND l.amount > :amount) OR " +
            "(:amountComparisonType = 'GREATER_THAN_OR_EQUAL' AND l.amount >= :amount)) " +
            "AND (:status IS NULL OR l.loanStatus = :status)"+
            "AND (:idBank IS NULL OR l.bank.id = :idBank)"+
            "AND (:idUser IS NULL OR l.idUser = :idUser)")
    Page<LoanDbDto> findLoanWithFilters(
            @Param("globalFilter") String globalFilter,
            @Param("name") String name,
            @Param("idBank") Integer idBank,
            @Param("date") LocalDate date,
            @Param("dateComparisonType") String dateComparisonType,
            @Param("amount") BigDecimal amount,
            @Param("amountComparisonType") String amountComparisonType,
            @Param("status") PaymentStatus status,
            @Param("idUser") Integer idUser, Pageable pageable);
}
