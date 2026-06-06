package net.focik.homeoffice.finance.domain.transaction.port.primary;

import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;

import java.time.LocalDate;
import java.util.List;

public interface GetBankTransactionUseCase {
    BankTransaction findById(int id);
    List<BankTransaction> findBetween(LocalDate dateFrom, LocalDate dateTo, int idUser);
}
