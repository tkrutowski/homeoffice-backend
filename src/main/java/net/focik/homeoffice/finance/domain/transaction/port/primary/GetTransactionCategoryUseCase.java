package net.focik.homeoffice.finance.domain.transaction.port.primary;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;

import java.util.List;

public interface GetTransactionCategoryUseCase {
    List<TransactionCategory> getAllTransactionCategories();
    TransactionCategory getTransactionCategoryById(int id);
}
