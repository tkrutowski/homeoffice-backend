package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.domain.exception.LoanProposalAlreadyHandledException;
import net.focik.homeoffice.finance.domain.exception.LoanProposalNotFoundException;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.port.primary.AddLoanUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.AcceptLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.DeleteLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.GetLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.IgnoreLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.IngestLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanEmailArchivePort;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanProposalRepository;
import net.focik.homeoffice.utils.UserHelper;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agreguje wszystkie UseCase'y dotyczące LoanProposal (ingest, odczyt, accept, ignore, delete) -
 * orchestracja: accept deleguje do {@link AddLoanUseCase}, ingest deleguje uruchomienie
 * ekstrakcji do {@link LoanProposalExtractionRunner} (async, żeby nie blokować webhooka n8n).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanProposalFacade implements IngestLoanProposalUseCase, GetLoanProposalUseCase,
        AcceptLoanProposalUseCase, IgnoreLoanProposalUseCase, DeleteLoanProposalUseCase {

    private final LoanProposalRepository loanProposalRepository;
    private final LoanEmailArchivePort loanEmailArchivePort;
    private final LoanProposalExtractionRunner extractionRunner;
    private final AddLoanUseCase addLoanUseCase;

    @Override
    public LoanProposal ingest(RawLoanEmail email) {
        var existing = loanProposalRepository.findBySourceMessageId(email.getMessageId());
        if (existing.isPresent()) {
            log.info("LoanProposal for messageId={} already exists (id={}), skipping duplicate ingest",
                    email.getMessageId(), existing.get().getId());
            return existing.get();
        }

        String s3Key = loanEmailArchivePort.store(email.getMessageId(), email.getRawEml()).orElse(null);

        LoanProposal proposal = LoanProposal.builder()
                .sourceMessageId(email.getMessageId())
                .sourceEmailFrom(email.getFrom())
                .sourceSubject(email.getSubject())
                .sourceFileS3Key(s3Key)
                .status(LoanProposalStatus.NEW)
                .receivedAt(LocalDateTime.now())
                .build();

        LoanProposal saved = loanProposalRepository.save(proposal);
        log.info("Ingested loan proposal id={} messageId={}", saved.getId(), saved.getSourceMessageId());

        extractionRunner.runAsync(saved.getId(), resolvePlainText(email));

        return saved;
    }

    private String resolvePlainText(RawLoanEmail email) {
        if (email.getTextBody() != null && !email.getTextBody().isBlank()) {
            return email.getTextBody();
        }
        if (email.getHtmlBody() != null && !email.getHtmlBody().isBlank()) {
            return Jsoup.parse(email.getHtmlBody()).text();
        }
        return "";
    }

    @Override
    public LoanProposal getLoanProposalById(int id) {
        return loanProposalRepository.findById(id)
                .orElseThrow(() -> new LoanProposalNotFoundException(id));
    }

    @Override
    public List<LoanProposal> getLoanProposalsByStatus(LoanProposalStatus status) {
        return loanProposalRepository.findByStatus(status);
    }

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Loan")
    public Loan accept(int proposalId, Loan finalLoan) {
        LoanProposal proposal = getLoanProposalById(proposalId);
        requireNotYetHandled(proposal);

        Loan createdLoan = addLoanUseCase.addLoan(finalLoan);

        proposal.markAccepted(createdLoan.getId(), UserHelper.getCurrentUserId());
        loanProposalRepository.save(proposal);

        log.info("LoanProposal id={} accepted, created loan id={}", proposalId, createdLoan.getId());
        return createdLoan;
    }

    @Override
    public LoanProposal ignore(int proposalId) {
        LoanProposal proposal = getLoanProposalById(proposalId);
        requireNotYetHandled(proposal);

        proposal.markIgnored(UserHelper.getCurrentUserId());
        LoanProposal saved = loanProposalRepository.save(proposal);

        log.info("LoanProposal id={} ignored", proposalId);
        return saved;
    }

    @Override
    public void deleteLoanProposalById(int id) {
        getLoanProposalById(id); // 404 gdy nie istnieje
        loanProposalRepository.deleteById(id);
        log.info("LoanProposal id={} deleted", id);
    }

    private void requireNotYetHandled(LoanProposal proposal) {
        if (proposal.getStatus() == LoanProposalStatus.ACCEPTED || proposal.getStatus() == LoanProposalStatus.IGNORED) {
            throw new LoanProposalAlreadyHandledException(proposal.getId());
        }
    }
}
