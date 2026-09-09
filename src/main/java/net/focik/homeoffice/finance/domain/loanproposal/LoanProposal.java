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
    /**
     * Kandydat na zakup wyekstrahowany z tego samego maila co {@code proposedLoan}, wypełniany
     * tylko gdy e-mail dotyczy finansowania konkretnego zakupu (np. PayPo, Allegro), nie zwykłego
     * kredytu bankowego - patrz {@code LoanProposalExtractionService.buildPurchaseCandidate}.
     * Użytkownik wybiera na froncie, czy zaksięgować propozycję jako kredyt czy jako zakup.
     */
    private ProposedPurchaseData proposedPurchase;
    private String failureReason;
    /** Ustawiane dopiero po accept() - id realnego Loan powstałego z tej propozycji. */
    private Integer createdLoanId;
    /** Ustawiane dopiero po acceptAsPurchase() - id realnego Purchase powstałego z tej propozycji. */
    private Integer createdPurchaseId;
    private LocalDateTime receivedAt;
    private LocalDateTime handledAt;
    private Integer handledByUserId;

    public void markExtracted(ProposedLoanData proposedLoan, ProposedPurchaseData proposedPurchase) {
        this.proposedLoan = proposedLoan;
        this.proposedPurchase = proposedPurchase;
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

    public void markAcceptedAsPurchase(int createdPurchaseId, int handledByUserId) {
        this.createdPurchaseId = createdPurchaseId;
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
