package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;

public class LoanProposalNotFoundException extends ObjectNotFoundException {
    public LoanProposalNotFoundException(Integer id) {
        super("LoanProposal with id = " + id + " does not exist");
    }
}
