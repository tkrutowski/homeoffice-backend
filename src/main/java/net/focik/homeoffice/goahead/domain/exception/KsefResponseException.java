package net.focik.homeoffice.goahead.domain.exception;

public class KsefResponseException extends RuntimeException {
    public KsefResponseException(String message) {
        super(message);
    }
    public KsefResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}