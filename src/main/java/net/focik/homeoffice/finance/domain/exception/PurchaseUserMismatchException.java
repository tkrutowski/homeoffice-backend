package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

public class PurchaseUserMismatchException extends ObjectNotValidException {
    public PurchaseUserMismatchException() {
        super("All selected purchases (and the resulting loan) must belong to the same user");
    }
}
