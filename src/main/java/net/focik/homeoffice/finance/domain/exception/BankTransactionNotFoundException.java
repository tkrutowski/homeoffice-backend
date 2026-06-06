package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;

public class BankTransactionNotFoundException extends ObjectNotFoundException {
    public BankTransactionNotFoundException(Integer id) {
        super("Bank transaction with id = " + id + " does not exist");
    }
}
