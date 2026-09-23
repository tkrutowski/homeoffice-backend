package net.focik.homeoffice.library.domain.exception;

import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.utils.exceptions.ObjectAlreadyExistException;

public class UserBookAlreadyExistException extends ObjectAlreadyExistException {
    public UserBookAlreadyExistException(Book book) {
        super("Książka '" + book.getTitle() + "' jest już na Twojej półce w statusie 'W poczekalni' lub 'Czytana'. " +
                "Zaktualizuj istniejący wpis zamiast dodawać nowy.");
    }

    public UserBookAlreadyExistException(String message) {
        super(message);
    }
}
