package net.focik.homeoffice.finance.domain.transaction.port.primary;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionImportResult;

public interface ImportBankTransactionsUseCase {
    TransactionImportResult importFromCsv(byte[] fileContent, int idUser, boolean testMode);
}
