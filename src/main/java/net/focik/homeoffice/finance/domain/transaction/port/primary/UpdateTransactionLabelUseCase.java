package net.focik.homeoffice.finance.domain.transaction.port.primary;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;

public interface UpdateTransactionLabelUseCase {
    TransactionLabel updateTransactionLabel(TransactionLabel transactionLabel);
}
