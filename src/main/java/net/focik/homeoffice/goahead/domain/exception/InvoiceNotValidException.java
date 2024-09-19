package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

public class InvoiceNotValidException extends ObjectNotValidException {
    public InvoiceNotValidException(String message) {
        super(message);
    }
}
