package net.focik.homeoffice.finance.domain.transaction.port.primary;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;

public interface AddTransactionCategoryUseCase {
    TransactionCategory addTransactionCategory(TransactionCategory transactionCategory);
}
