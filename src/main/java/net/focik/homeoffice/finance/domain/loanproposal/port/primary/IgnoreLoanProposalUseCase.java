package net.focik.homeoffice.finance.domain.loanproposal.port.primary;

import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;

public interface IgnoreLoanProposalUseCase {
    LoanProposal ignore(int proposalId);
}
