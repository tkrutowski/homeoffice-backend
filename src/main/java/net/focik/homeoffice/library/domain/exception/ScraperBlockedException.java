package net.focik.homeoffice.library.domain.exception;

//@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class ScraperBlockedException extends RuntimeException {
    public ScraperBlockedException(String message) {
        super(message);
    }
}
