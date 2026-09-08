package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.IngestLoanEmailRequest;
import net.focik.homeoffice.finance.api.dto.LoanProposalDto;
import net.focik.homeoffice.finance.api.dto.ProposedLoanDataDto;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.ProposedLoanData;
import net.focik.homeoffice.finance.domain.loanproposal.RawLoanEmail;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class ApiLoanProposalMapper {

    public LoanProposalDto toDto(LoanProposal p) {
        return LoanProposalDto.builder()
                .id(p.getId())
                .sourceEmailFrom(p.getSourceEmailFrom())
                .sourceSubject(p.getSourceSubject())
                .sourceFileS3Key(p.getSourceFileS3Key())
                .status(p.getStatus())
                .proposedLoan(toDto(p.getProposedLoan()))
                .failureReason(p.getFailureReason())
                .createdLoanId(p.getCreatedLoanId())
                .receivedAt(p.getReceivedAt())
                .handledAt(p.getHandledAt())
                .build();
    }

    private ProposedLoanDataDto toDto(ProposedLoanData d) {
        if (d == null) {
            return null;
        }
        return ProposedLoanDataDto.builder()
                .originalSenderEmail(d.getOriginalSenderEmail())
                .bankId(d.getBankId())
                .bankName(d.getBankName())
                .name(d.getName())
                .amount(d.getAmount())
                .date(d.getDate())
                .loanNumber(d.getLoanNumber())
                .accountNumber(d.getAccountNumber())
                .firstPaymentDate(d.getFirstPaymentDate())
                .numberOfInstallments(d.getNumberOfInstallments())
                .installmentAmount(d.getInstallmentAmount())
                .loanCost(d.getLoanCost())
                .otherInfo(d.getOtherInfo())
                .build();
    }

    public RawLoanEmail toDomain(IngestLoanEmailRequest r) {
        return RawLoanEmail.builder()
                .messageId(r.getMessageId())
                .from(r.getFrom())
                .subject(r.getSubject())
                .textBody(r.getTextBody())
                .htmlBody(r.getHtmlBody())
                .rawEml(decodeRawEml(r.getRawEmlBase64()))
                .build();
    }

    private byte[] decodeRawEml(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        return Base64.getDecoder().decode(base64);
    }
}
