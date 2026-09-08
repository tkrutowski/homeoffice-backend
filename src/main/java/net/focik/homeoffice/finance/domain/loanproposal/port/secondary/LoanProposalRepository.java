package net.focik.homeoffice.finance.domain.loanproposal.port.secondary;

import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface LoanProposalRepository {

    LoanProposal save(LoanProposal loanProposal);

    Optional<LoanProposal> findById(Integer id);

    Optional<LoanProposal> findBySourceMessageId(String sourceMessageId);

    List<LoanProposal> findByStatus(LoanProposalStatus status);

    void deleteById(int id);
}
