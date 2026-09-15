package net.focik.homeoffice.library.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class BookstoreCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public BookstoreCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć księgarnię musisz najpierw usunąć %s", needToRemove));
    }
}
