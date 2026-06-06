package net.focik.homeoffice.finance.domain.transaction.port.secondary;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;

import java.util.List;
import java.util.Optional;

public interface TransactionCategoryRepository {
    TransactionCategory saveTransactionCategory(TransactionCategory transactionCategory);
    void deleteTransactionCategory(int id);
    List<TransactionCategory> findAllTransactionCategories();
    Optional<TransactionCategory> findTransactionCategoryById(int id);
}
