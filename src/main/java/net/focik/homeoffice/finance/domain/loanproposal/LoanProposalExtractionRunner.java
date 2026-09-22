package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanProposalRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Odpala ekstrakcję poza wątkiem HTTP, żeby POST /internal/loan-proposals/ingest (webhook n8n)
 * nie czekał na odpowiedź Claude. Osobny bean od {@link LoanProposalFacade}, bo @Async na
 * wywołaniu z tego samego beana (self-invocation) zostałoby zignorowane przez proxy Springa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanProposalExtractionRunner {

    private final LoanProposalRepository loanProposalRepository;
    private final LoanProposalExtractionService extractionService;

    @Async
    public void runAsync(int proposalId, String emailText) {
        LoanProposal proposal = loanProposalRepository.findById(proposalId).orElse(null);
        if (proposal == null) {
            log.warn("LoanProposal id={} disappeared before extraction could run", proposalId);
            return;
        }

        try {
            // sourceSubject/idUser sa juz ustalone na proposal (temat maila przy ingest, idUser przez
            // dopasowanie nadawcy) - przekazujemy je do ekstrakcji, zeby dopasowac karte/firme/usera
            // do draftu zakupu bez osobnego zapytania frontu.
            ExtractedProposals extracted = extractionService.extract(emailText, proposal.getSourceSubject(), proposal.getIdUser());
            if (!extracted.isEmpty()) {
                proposal.markExtracted(extracted.loan().orElse(null), extracted.purchase().orElse(null));
                log.info("LoanProposal id={} extracted successfully (loan={}, purchase={})",
                        proposalId, extracted.loan().isPresent(), extracted.purchase().isPresent());
            } else {
                proposal.markFailed("Nie rozpoznano wiadomości jako dokumentu kredytowego");
                log.info("LoanProposal id={} not recognized as a loan document", proposalId);
            }
        } catch (Exception e) {
            log.error("Extraction failed for LoanProposal id={}", proposalId, e);
            proposal.markFailed("Błąd ekstrakcji: " + e.getMessage());
        }

        loanProposalRepository.save(proposal);
    }
}
