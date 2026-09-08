package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

public class LoanProposalAlreadyHandledException extends ObjectNotValidException {
    public LoanProposalAlreadyHandledException(Integer id) {
        super("LoanProposal with id = " + id + " has already been handled");
    }
}
