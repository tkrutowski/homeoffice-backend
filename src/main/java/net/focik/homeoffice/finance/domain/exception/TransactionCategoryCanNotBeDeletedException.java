package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class TransactionCategoryCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public TransactionCategoryCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć kategorię transakcji musisz najpierw usunąć %s", needToRemove));
    }
}
