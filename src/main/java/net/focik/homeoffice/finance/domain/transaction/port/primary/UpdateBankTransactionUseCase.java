package net.focik.homeoffice.finance.domain.transaction.port.primary;

import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;

public interface UpdateBankTransactionUseCase {
    BankTransaction updateBankTransaction(BankTransaction bankTransaction);
}
