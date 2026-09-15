package net.focik.homeoffice.library.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class SeriesCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public SeriesCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć serię musisz najpierw usunąć %s", needToRemove));
    }
}
