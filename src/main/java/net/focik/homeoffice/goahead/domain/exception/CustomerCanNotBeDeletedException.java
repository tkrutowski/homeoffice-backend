package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class CustomerCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public CustomerCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć klienta musisz najpierw usunąć %s", needToRemove));
    }
}
