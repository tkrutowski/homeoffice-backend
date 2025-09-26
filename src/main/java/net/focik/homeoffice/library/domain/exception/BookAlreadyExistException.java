package net.focik.homeoffice.library.domain.exception;

import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.utils.exceptions.ObjectAlreadyExistException;

public class BookAlreadyExistException extends ObjectAlreadyExistException {
    public BookAlreadyExistException(Book book) {
        super("Książka pod tytułem '" + book.getTitle() + "' już istnieje w bazie danych.");
    }
    public BookAlreadyExistException(String message){
        super(message);
    }
}
