package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectAlreadyExistException;

public class CardAlreadyExistException extends ObjectAlreadyExistException {
    public CardAlreadyExistException(String message) {
        super(message);
    }
}
