package net.focik.homeoffice.finance.domain.loanproposal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoanProposalTest {

    @Test
    void markExtracted_ShouldSetBothCandidates_WhenBothPresent() {
        LoanProposal proposal = LoanProposal.builder().status(LoanProposalStatus.NEW).build();
        ProposedLoanData loan = ProposedLoanData.builder().bankName("PayPo").build();
        ProposedPurchaseData purchase = ProposedPurchaseData.builder().name("Sklep XYZ").build();

        proposal.markExtracted(loan, purchase);

        assertThat(proposal.getStatus()).isEqualTo(LoanProposalStatus.EXTRACTED);
        assertThat(proposal.getProposedLoan()).isEqualTo(loan);
        assertThat(proposal.getProposedPurchase()).isEqualTo(purchase);
    }

    @Test
    void markExtracted_ShouldAllowNullPurchase_WhenOnlyLoanRecognized() {
        LoanProposal proposal = LoanProposal.builder().status(LoanProposalStatus.NEW).build();
        ProposedLoanData loan = ProposedLoanData.builder().bankName("mBank").build();

        proposal.markExtracted(loan, null);

        assertThat(proposal.getStatus()).isEqualTo(LoanProposalStatus.EXTRACTED);
        assertThat(proposal.getProposedLoan()).isEqualTo(loan);
        assertThat(proposal.getProposedPurchase()).isNull();
    }

    @Test
    void markAcceptedAsPurchase_ShouldSetCreatedPurchaseIdAndStatus_AndLeaveCreatedLoanIdNull() {
        LoanProposal proposal = LoanProposal.builder().status(LoanProposalStatus.EXTRACTED).build();

        proposal.markAcceptedAsPurchase(42, 7);

        assertThat(proposal.getStatus()).isEqualTo(LoanProposalStatus.ACCEPTED);
        assertThat(proposal.getCreatedPurchaseId()).isEqualTo(42);
        assertThat(proposal.getCreatedLoanId()).isNull();
        assertThat(proposal.getHandledByUserId()).isEqualTo(7);
        assertThat(proposal.getHandledAt()).isNotNull();
    }

    @Test
    void markAccepted_ShouldLeaveCreatedPurchaseIdNull_WhenAcceptedAsLoan() {
        LoanProposal proposal = LoanProposal.builder().status(LoanProposalStatus.EXTRACTED).build();

        proposal.markAccepted(99, 7);

        assertThat(proposal.getStatus()).isEqualTo(LoanProposalStatus.ACCEPTED);
        assertThat(proposal.getCreatedLoanId()).isEqualTo(99);
        assertThat(proposal.getCreatedPurchaseId()).isNull();
    }
}
