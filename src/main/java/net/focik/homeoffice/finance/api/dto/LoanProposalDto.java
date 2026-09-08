package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class LoanProposalDto {
    private int id;
    private String sourceEmailFrom;
    private String sourceSubject;
    private String sourceFileS3Key;
    private LoanProposalStatus status;
    private ProposedLoanDataDto proposedLoan;
    private String failureReason;
    private Integer createdLoanId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Europe/Warsaw")
    private LocalDateTime receivedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Europe/Warsaw")
    private LocalDateTime handledAt;
}
