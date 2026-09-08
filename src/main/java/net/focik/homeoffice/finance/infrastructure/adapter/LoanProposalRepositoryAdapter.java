package net.focik.homeoffice.finance.infrastructure.adapter;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanProposalRepository;
import net.focik.homeoffice.finance.infrastructure.jpa.LoanProposalDtoRepository;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaLoanProposalMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
class LoanProposalRepositoryAdapter implements LoanProposalRepository {

    LoanProposalDtoRepository loanProposalDtoRepository;
    JpaLoanProposalMapper mapper;

    @Override
    public LoanProposal save(LoanProposal loanProposal) {
        return mapper.toDomain(loanProposalDtoRepository.save(mapper.toDto(loanProposal)));
    }

    @Override
    public Optional<LoanProposal> findById(Integer id) {
        return loanProposalDtoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LoanProposal> findBySourceMessageId(String sourceMessageId) {
        return loanProposalDtoRepository.findBySourceMessageId(sourceMessageId).map(mapper::toDomain);
    }

    @Override
    public List<LoanProposal> findByStatus(LoanProposalStatus status) {
        return loanProposalDtoRepository.findAllByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(int id) {
        loanProposalDtoRepository.deleteById(id);
    }
}
