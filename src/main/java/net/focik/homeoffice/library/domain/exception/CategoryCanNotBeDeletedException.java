package net.focik.homeoffice.library.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class CategoryCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public CategoryCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć kategorię musisz najpierw usunąć %s", needToRemove));
    }
}
