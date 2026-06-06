package net.focik.homeoffice.finance.domain.transaction.port.secondary;

import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository {
    BankTransaction saveBankTransaction(BankTransaction bankTransaction);
    Optional<BankTransaction> findBankTransactionById(Integer id);
    List<BankTransaction> findBankTransactionByUserId(Integer idUser);
    List<BankTransaction> findBankTransactionBetween(Integer idUser, LocalDate dateFrom, LocalDate dateTo);
    void deleteBankTransactionById(int id);
    List<BankTransaction> findAll();
}
