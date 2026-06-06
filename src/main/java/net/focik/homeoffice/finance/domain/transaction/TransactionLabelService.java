package net.focik.homeoffice.finance.domain.transaction;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.TransactionLabelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
class TransactionLabelService {

    private final TransactionLabelRepository transactionLabelRepository;

    TransactionLabel addTransactionLabel(TransactionLabel transactionLabel) {
        transactionLabel.setId(null);
        return transactionLabelRepository.saveTransactionLabel(transactionLabel);
    }

    TransactionLabel updateTransactionLabel(TransactionLabel transactionLabel) {
        return transactionLabelRepository.saveTransactionLabel(transactionLabel);
    }

    void deleteTransactionLabel(int id) {
        transactionLabelRepository.deleteTransactionLabel(id);
    }

    List<TransactionLabel> findAllTransactionLabels() {
        return transactionLabelRepository.findAllTransactionLabels();
    }

    TransactionLabel findTransactionLabelById(int id) {
        return transactionLabelRepository.findTransactionLabelById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction label not found with id: " + id));
    }
}
