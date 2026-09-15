package net.focik.homeoffice.finance.domain.transaction;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.exception.TransactionCategoryCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategoryType;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.BankTransactionRepository;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.TransactionCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
class TransactionCategoryService {

    private final TransactionCategoryRepository transactionCategoryRepository;
    private final BankTransactionRepository bankTransactionRepository;

    TransactionCategory addTransactionCategory(TransactionCategory transactionCategory) {
        transactionCategory.setId(null);
        return transactionCategoryRepository.saveTransactionCategory(transactionCategory);
    }

    TransactionCategory updateTransactionCategory(TransactionCategory transactionCategory) {
        return transactionCategoryRepository.saveTransactionCategory(transactionCategory);
    }

    void deleteTransactionCategory(int id) {
        if (bankTransactionRepository.existsByTransactionCategory(id)) {
            log.warn("Transaction category with ID {} cannot be deleted — associated bank transactions found.", id);
            throw new TransactionCategoryCanNotBeDeletedException("transakcje bankowe.");
        }
        transactionCategoryRepository.deleteTransactionCategory(id);
    }

    List<TransactionCategory> findAllTransactionCategories() {
        return transactionCategoryRepository.findAllTransactionCategories();
    }

    TransactionCategory findTransactionCategoryById(int id) {
        return transactionCategoryRepository.findTransactionCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction category not found with id: " + id));
    }

    Optional<TransactionCategory> findCategoryByName(String name) {
        return transactionCategoryRepository.findCategoryByName(name);
    }

    public Optional<TransactionCategory> findCategoryByNameAndType(String categoryName, TransactionCategoryType type) {
        try {
            return transactionCategoryRepository.findCategoryByNameAndType(categoryName, type);
        } catch (Exception e) {
            log.error("Error while finding category by name and type: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
