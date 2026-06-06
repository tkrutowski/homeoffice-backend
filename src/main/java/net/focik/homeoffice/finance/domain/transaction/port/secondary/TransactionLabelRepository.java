package net.focik.homeoffice.finance.domain.transaction.port.secondary;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;

import java.util.List;
import java.util.Optional;

public interface TransactionLabelRepository {
    TransactionLabel saveTransactionLabel(TransactionLabel transactionLabel);
    void deleteTransactionLabel(int id);
    List<TransactionLabel> findAllTransactionLabels();
    Optional<TransactionLabel> findTransactionLabelById(int id);
}
