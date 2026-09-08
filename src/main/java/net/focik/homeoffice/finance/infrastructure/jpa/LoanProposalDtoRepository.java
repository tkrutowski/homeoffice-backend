package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;
import net.focik.homeoffice.finance.infrastructure.dto.LoanProposalDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProposalDtoRepository extends JpaRepository<LoanProposalDbDto, Integer> {

    Optional<LoanProposalDbDto> findBySourceMessageId(String sourceMessageId);

    List<LoanProposalDbDto> findAllByStatus(LoanProposalStatus status);
}
