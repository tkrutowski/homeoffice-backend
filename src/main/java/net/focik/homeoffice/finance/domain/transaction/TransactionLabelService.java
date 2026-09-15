package net.focik.homeoffice.finance.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.exception.TransactionLabelCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.BankTransactionRepository;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.TransactionLabelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
class TransactionLabelService {

    private final TransactionLabelRepository transactionLabelRepository;
    private final BankTransactionRepository bankTransactionRepository;

    TransactionLabel addTransactionLabel(TransactionLabel transactionLabel) {
        transactionLabel.setId(null);
        return transactionLabelRepository.saveTransactionLabel(transactionLabel);
    }

    TransactionLabel updateTransactionLabel(TransactionLabel transactionLabel) {
        return transactionLabelRepository.saveTransactionLabel(transactionLabel);
    }

    void deleteTransactionLabel(int id) {
        if (bankTransactionRepository.existsByTransactionLabel(id)) {
            log.warn("Transaction label with ID {} cannot be deleted — associated bank transactions found.", id);
            throw new TransactionLabelCanNotBeDeletedException("transakcje bankowe.");
        }
        transactionLabelRepository.deleteTransactionLabel(id);
    }

    List<TransactionLabel> findAllTransactionLabels() {
        return transactionLabelRepository.findAllTransactionLabels();
    }

    TransactionLabel findTransactionLabelById(int id) {
        return transactionLabelRepository.findTransactionLabelById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction label not found with id: " + id));
    }

    public Optional<TransactionLabel> findLabelByName(String trimmedName) {
        return transactionLabelRepository.findLabelByName(trimmedName);
    }
}
