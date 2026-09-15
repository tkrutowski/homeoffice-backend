package net.focik.homeoffice.goahead.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class SupplierCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public SupplierCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć dostawcę musisz najpierw usunąć %s", needToRemove));
    }
}
