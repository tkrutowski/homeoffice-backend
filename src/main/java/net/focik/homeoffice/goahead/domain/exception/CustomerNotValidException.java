package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

public class CustomerNotValidException extends ObjectNotValidException {
    public CustomerNotValidException(String message) {
        super(message);
    }
}
