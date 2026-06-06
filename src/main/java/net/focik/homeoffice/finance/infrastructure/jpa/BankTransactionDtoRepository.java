package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.BankTransactionDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BankTransactionDtoRepository extends JpaRepository<BankTransactionDbDto, Integer> {
    List<BankTransactionDbDto> findAllByIdUser(Integer idUser);

    @Query("SELECT bt FROM BankTransactionDbDto bt WHERE bt.idUser = :idUser AND bt.transactionDate BETWEEN :dateFrom AND :dateTo ORDER BY bt.transactionDate DESC")
    List<BankTransactionDbDto> findByUserAndDateRange(Integer idUser, LocalDate dateFrom, LocalDate dateTo);
}
