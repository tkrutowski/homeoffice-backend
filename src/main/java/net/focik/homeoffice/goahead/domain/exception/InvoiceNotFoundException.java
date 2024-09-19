package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;

public class InvoiceNotFoundException extends ObjectNotFoundException {
    public InvoiceNotFoundException(String message) {
        super(message);
    }
}
