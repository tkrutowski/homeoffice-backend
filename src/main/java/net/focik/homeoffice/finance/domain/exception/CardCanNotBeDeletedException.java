package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class CardCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public CardCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć kartę musisz najpierw usunąć %s", needToRemove));
    }

}
