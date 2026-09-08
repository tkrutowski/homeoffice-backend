package net.focik.homeoffice.finance.infrastructure.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.ProposedLoanData;
import net.focik.homeoffice.finance.infrastructure.dto.LoanProposalDbDto;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaLoanProposalMapper {

    private final ObjectMapper objectMapper;

    public LoanProposalDbDto toDto(LoanProposal proposal) {
        return LoanProposalDbDto.builder()
                .id(proposal.getId() == 0 ? null : proposal.getId())
                .sourceMessageId(proposal.getSourceMessageId())
                .sourceEmailFrom(proposal.getSourceEmailFrom())
                .sourceSubject(proposal.getSourceSubject())
                .sourceFileS3Key(proposal.getSourceFileS3Key())
                .status(proposal.getStatus())
                .proposedLoanJson(writeJson(proposal.getProposedLoan()))
                .failureReason(proposal.getFailureReason())
                .createdLoanId(proposal.getCreatedLoanId())
                .handledAt(proposal.getHandledAt())
                .handledByUserId(proposal.getHandledByUserId())
                .build();
    }

    public LoanProposal toDomain(LoanProposalDbDto dto) {
        return LoanProposal.builder()
                .id(dto.getId())
                .sourceMessageId(dto.getSourceMessageId())
                .sourceEmailFrom(dto.getSourceEmailFrom())
                .sourceSubject(dto.getSourceSubject())
                .sourceFileS3Key(dto.getSourceFileS3Key())
                .status(dto.getStatus())
                .proposedLoan(readJson(dto.getProposedLoanJson()))
                .failureReason(dto.getFailureReason())
                .createdLoanId(dto.getCreatedLoanId())
                .receivedAt(dto.getCreatedAt())
                .handledAt(dto.getHandledAt())
                .handledByUserId(dto.getHandledByUserId())
                .build();
    }

    private String writeJson(ProposedLoanData data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ProposedLoanData", e);
            return null;
        }
    }

    private ProposedLoanData readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ProposedLoanData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ProposedLoanData: {}", json, e);
            return null;
        }
    }
}
