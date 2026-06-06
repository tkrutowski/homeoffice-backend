package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

public class BankTransactionNotValidException extends ObjectNotValidException {
    public BankTransactionNotValidException(String message) {
        super(message);
    }

    public BankTransactionNotValidException() {
        super("Bank transaction variable can't be null or empty");
    }
}
