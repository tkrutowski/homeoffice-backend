package net.focik.homeoffice.goahead.domain.exception;


import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;

public class InvoiceItemNotFoundException extends ObjectNotFoundException {
    public InvoiceItemNotFoundException(String message) {
        super(message);
    }
}
