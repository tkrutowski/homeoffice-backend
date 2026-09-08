package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Propozycja kredytu wygenerowana z przychodzącego e-maila (np. z PayPo). Trwały rekord,
 * który wiąże konkretną wiadomość z jej obsłużeniem - status i createdLoanId są jedynym
 * źródłem prawdy o tym, czy dana wiadomość została już obsłużona, niezależnie od stanu skrzynki.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class LoanProposal {
    private int id;
    private String sourceMessageId;
    private String sourceEmailFrom;
    private String sourceSubject;
    private String sourceFileS3Key;
    private LoanProposalStatus status;
    private ProposedLoanData proposedLoan;
    private String failureReason;
    /** Ustawiane dopiero po accept() - id realnego Loan powstałego z tej propozycji. */
    private Integer createdLoanId;
    private LocalDateTime receivedAt;
    private LocalDateTime handledAt;
    private Integer handledByUserId;

    public void markExtracted(ProposedLoanData proposedLoan) {
        this.proposedLoan = proposedLoan;
        this.status = LoanProposalStatus.EXTRACTED;
    }

    public void markFailed(String reason) {
        this.failureReason = reason;
        this.status = LoanProposalStatus.FAILED;
    }

    public void markAccepted(int createdLoanId, int handledByUserId) {
        this.createdLoanId = createdLoanId;
        this.status = LoanProposalStatus.ACCEPTED;
        this.handledByUserId = handledByUserId;
        this.handledAt = LocalDateTime.now();
    }

    public void markIgnored(int handledByUserId) {
        this.status = LoanProposalStatus.IGNORED;
        this.handledByUserId = handledByUserId;
        this.handledAt = LocalDateTime.now();
    }
}
