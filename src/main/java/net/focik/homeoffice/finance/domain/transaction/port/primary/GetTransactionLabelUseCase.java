package net.focik.homeoffice.finance.domain.transaction.port.primary;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;

import java.util.List;

public interface GetTransactionLabelUseCase {
    List<TransactionLabel> getAllTransactionLabels();
    TransactionLabel getTransactionLabelById(int id);
}
