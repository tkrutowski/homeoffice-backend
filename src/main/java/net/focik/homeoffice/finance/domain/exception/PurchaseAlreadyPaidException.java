package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

public class PurchaseAlreadyPaidException extends ObjectNotValidException {
    public PurchaseAlreadyPaidException(Integer purchaseId) {
        super("Purchase with id = " + purchaseId + " is already paid and can't be converted to a loan");
    }
}
