package net.focik.homeoffice.library.domain.exception;

/** Nie udało się zamienić tekstu użytkownika na zapytanie o książkę (np. model AI jest niedostępny). */
public class BookQueryExtractionException extends RuntimeException {
    public BookQueryExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
