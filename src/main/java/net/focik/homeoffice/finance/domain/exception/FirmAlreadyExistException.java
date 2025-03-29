package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectAlreadyExistException;

public class FirmAlreadyExistException extends ObjectAlreadyExistException {
    public FirmAlreadyExistException(String message) {
        super(message);
    }
}
