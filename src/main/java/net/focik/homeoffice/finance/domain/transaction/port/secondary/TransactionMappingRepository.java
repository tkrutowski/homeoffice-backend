package net.focik.homeoffice.finance.domain.transaction.port.secondary;

import java.util.Optional;

public interface TransactionMappingRepository {
    Optional<Integer> findCategoryIdByName(String categoryName);
    Optional<Integer> findLabelIdByName(String labelName);
}
