package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectAlreadyExistException;

public class InvoiceAlreadyExistException extends ObjectAlreadyExistException {
    public InvoiceAlreadyExistException(String message) {
        super(message);
    }
}
