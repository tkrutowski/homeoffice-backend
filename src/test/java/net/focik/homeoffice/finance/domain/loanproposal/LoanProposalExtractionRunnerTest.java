package net.focik.homeoffice.finance.domain.loanproposal;

import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanProposalExtractionRunnerTest {

    @Mock
    private LoanProposalRepository loanProposalRepository;

    @Mock
    private LoanProposalExtractionService extractionService;

    private LoanProposalExtractionRunner runner;

    @BeforeEach
    void setUp() {
        runner = new LoanProposalExtractionRunner(loanProposalRepository, extractionService);
    }

    @Test
    @DisplayName("should mark the proposal extracted with both candidates when loan and purchase are recognized")
    void runAsync_ShouldMarkExtractedWithBothCandidates_WhenLoanAndPurchaseRecognized() {
        LoanProposal proposal = LoanProposal.builder().id(1).idUser(7).sourceSubject("Subject").status(LoanProposalStatus.NEW).build();
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        ProposedLoanData loan = ProposedLoanData.builder().bankName("PayPo").build();
        ProposedPurchaseData purchase = ProposedPurchaseData.builder().name("Sklep XYZ").idUser(7).build();
        when(extractionService.extract("mail", "Subject", 7)).thenReturn(ExtractedProposals.of(loan, purchase));

        runner.runAsync(1, "mail");

        ArgumentCaptor<LoanProposal> captor = ArgumentCaptor.forClass(LoanProposal.class);
        verify(loanProposalRepository).save(captor.capture());
        LoanProposal saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(LoanProposalStatus.EXTRACTED);
        assertThat(saved.getProposedLoan()).isEqualTo(loan);
        assertThat(saved.getProposedPurchase()).isEqualTo(purchase);
    }

    @Test
    @DisplayName("should pass the proposal's subject and idUser through to the extraction service")
    void runAsync_ShouldPassSubjectAndIdUserToExtractionService() {
        LoanProposal proposal = LoanProposal.builder().id(1).status(LoanProposalStatus.NEW).build();
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        ProposedLoanData loan = ProposedLoanData.builder().bankName("mBank").build();
        when(extractionService.extract("mail", null, null)).thenReturn(ExtractedProposals.of(loan, null));

        runner.runAsync(1, "mail");

        verify(extractionService).extract("mail", null, null);
    }

    @Test
    @DisplayName("should mark the proposal extracted with the loan candidate only when the purchase is not applicable")
    void runAsync_ShouldMarkExtractedWithLoanOnly_WhenPurchaseNotApplicable() {
        LoanProposal proposal = LoanProposal.builder().id(1).status(LoanProposalStatus.NEW).build();
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        ProposedLoanData loan = ProposedLoanData.builder().bankName("mBank").build();
        when(extractionService.extract("mail", null, null)).thenReturn(ExtractedProposals.of(loan, null));

        runner.runAsync(1, "mail");

        ArgumentCaptor<LoanProposal> captor = ArgumentCaptor.forClass(LoanProposal.class);
        verify(loanProposalRepository).save(captor.capture());
        LoanProposal saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(LoanProposalStatus.EXTRACTED);
        assertThat(saved.getProposedLoan()).isEqualTo(loan);
        assertThat(saved.getProposedPurchase()).isNull();
    }

    @Test
    @DisplayName("should mark the proposal failed when nothing is recognized in the message")
    void runAsync_ShouldMarkFailed_WhenNothingRecognized() {
        LoanProposal proposal = LoanProposal.builder().id(1).status(LoanProposalStatus.NEW).build();
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        when(extractionService.extract("newsletter", null, null)).thenReturn(ExtractedProposals.none());

        runner.runAsync(1, "newsletter");

        ArgumentCaptor<LoanProposal> captor = ArgumentCaptor.forClass(LoanProposal.class);
        verify(loanProposalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(LoanProposalStatus.FAILED);
    }
}
