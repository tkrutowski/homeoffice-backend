package net.focik.homeoffice.finance.domain.loanproposal.port.primary;

import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;

import java.util.List;

public interface GetLoanProposalUseCase {
    LoanProposal getLoanProposalById(int id);

    List<LoanProposal> getLoanProposalsByStatus(LoanProposalStatus status);
}
