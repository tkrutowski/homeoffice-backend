package net.focik.homeoffice.library.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class AuthorCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public AuthorCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć autora musisz najpierw usunąć %s", needToRemove));
    }
}
