package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

public class PurchaseAlreadyLinkedToLoanException extends ObjectNotValidException {
    public PurchaseAlreadyLinkedToLoanException(Integer purchaseId, Integer loanId) {
        super("Purchase with id = " + purchaseId + " is already linked to loan with id = " + loanId);
    }
}
