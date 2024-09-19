package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;

public class CustomerNotFoundException extends ObjectNotFoundException {
    public CustomerNotFoundException(String field, String message) {
        super(String.format("Nie znaleziono klienta o %s: %s",field, message));
    }

}
