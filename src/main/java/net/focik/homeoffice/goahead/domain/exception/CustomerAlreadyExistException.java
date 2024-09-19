package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectAlreadyExistException;

public class CustomerAlreadyExistException extends ObjectAlreadyExistException {
    public CustomerAlreadyExistException(String message) {
        super(message);
    }
}
