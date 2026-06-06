package net.focik.homeoffice.finance.domain.transaction;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.TransactionCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
class TransactionCategoryService {

    private final TransactionCategoryRepository transactionCategoryRepository;

    TransactionCategory addTransactionCategory(TransactionCategory transactionCategory) {
        transactionCategory.setId(null);
        return transactionCategoryRepository.saveTransactionCategory(transactionCategory);
    }

    TransactionCategory updateTransactionCategory(TransactionCategory transactionCategory) {
        return transactionCategoryRepository.saveTransactionCategory(transactionCategory);
    }

    void deleteTransactionCategory(int id) {
        transactionCategoryRepository.deleteTransactionCategory(id);
    }

    List<TransactionCategory> findAllTransactionCategories() {
        return transactionCategoryRepository.findAllTransactionCategories();
    }

    TransactionCategory findTransactionCategoryById(int id) {
        return transactionCategoryRepository.findTransactionCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction category not found with id: " + id));
    }
}
