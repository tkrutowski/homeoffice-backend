package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class TransactionLabelCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public TransactionLabelCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć etykietę transakcji musisz najpierw usunąć %s", needToRemove));
    }
}
