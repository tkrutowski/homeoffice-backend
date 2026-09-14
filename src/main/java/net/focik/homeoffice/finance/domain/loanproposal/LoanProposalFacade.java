package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.domain.exception.LoanProposalAlreadyHandledException;
import net.focik.homeoffice.finance.domain.exception.LoanProposalNotFoundException;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.port.primary.AddLoanUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.AcceptLoanProposalAsPurchaseUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.AcceptLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.DeleteLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.GetLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.IgnoreLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.IngestLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanEmailArchivePort;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanProposalRepository;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.AddPurchaseUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import org.jsoup.Jsoup;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;

/**
 * Agreguje wszystkie UseCase'y dotyczące LoanProposal (ingest, odczyt, accept, ignore, delete) -
 * orchestracja: accept deleguje do {@link AddLoanUseCase}, ingest deleguje uruchomienie
 * ekstrakcji do {@link LoanProposalExtractionRunner} (async, żeby nie blokować webhooka n8n).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanProposalFacade implements IngestLoanProposalUseCase, GetLoanProposalUseCase,
        AcceptLoanProposalUseCase, AcceptLoanProposalAsPurchaseUseCase, IgnoreLoanProposalUseCase,
        DeleteLoanProposalUseCase {

    private static final Pattern EMAIL_IN_ANGLE_BRACKETS = Pattern.compile("<([^<>]+)>");

    private final LoanProposalRepository loanProposalRepository;
    private final LoanEmailArchivePort loanEmailArchivePort;
    private final LoanProposalExtractionRunner extractionRunner;
    private final AddLoanUseCase addLoanUseCase;
    private final AddPurchaseUseCase addPurchaseUseCase;
    private final UserFacade userFacade;

    @Override
    public LoanProposal ingest(RawLoanEmail email) {
        var existing = loanProposalRepository.findBySourceMessageId(email.getMessageId());
        if (existing.isPresent()) {
            log.info("LoanProposal for messageId={} already exists (id={}), skipping duplicate ingest",
                    email.getMessageId(), existing.get().getId());
            return existing.get();
        }

        String s3Key = loanEmailArchivePort.store(email.getMessageId(), email.getRawEml()).orElse(null);
        Integer resolvedIdUser = resolveIdUserFromEmail(email.getFrom());

        LoanProposal proposal = LoanProposal.builder()
                .idUser(resolvedIdUser)
                .sourceMessageId(email.getMessageId())
                .sourceEmailFrom(email.getFrom())
                .sourceSubject(email.getSubject())
                .sourceFileS3Key(s3Key)
                .status(LoanProposalStatus.NEW)
                .receivedAt(LocalDateTime.now())
                .build();

        LoanProposal saved = loanProposalRepository.save(proposal);
        if (resolvedIdUser == null) {
            log.warn("Ingested loan proposal id={} messageId={} - could not match sender '{}' to a user, " +
                    "will only be visible to privileged users", saved.getId(), saved.getSourceMessageId(), email.getFrom());
        } else {
            log.info("Ingested loan proposal id={} messageId={}, matched to user id={}",
                    saved.getId(), saved.getSourceMessageId(), resolvedIdUser);
        }

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

    /**
     * Proba dopasowania nadawcy przekierowania do istniejacego uzytkownika po adresie e-mail.
     * Gmail przy automatycznym przekierowaniu wg reguly nadawcy podmienia "From" na adres
     * skrzynki przekierowujacej (czyli domownika), wiec sourceEmailFrom to w praktyce adres
     * osoby, ktorej dotyczy propozycja - nie adres wierzyciela (PayPo/bank).
     */
    private Integer resolveIdUserFromEmail(String from) {
        if (from == null || from.isBlank()) {
            return null;
        }

        String email = extractEmailAddress(from);
        AppUser user = userFacade.findUserByEmail(email);
        return user != null ? Math.toIntExact(user.getId()) : null;
    }

    /**
     * "From" moze przyjsc jako sam adres albo jako "Imie Nazwisko &lt;adres@example.com&gt;"
     * (zaleznie od klienta IMAP w n8n) - wyciaga sam adres w obu przypadkach.
     */
    private String extractEmailAddress(String from) {
        Matcher matcher = EMAIL_IN_ANGLE_BRACKETS.matcher(from);
        return (matcher.find() ? matcher.group(1) : from).trim();
    }

    @Override
    public LoanProposal getLoanProposalById(int id) {
        LoanProposal proposal = loanProposalRepository.findById(id)
                .orElseThrow(() -> new LoanProposalNotFoundException(id));
        assertCanAccessProposal(proposal);
        return proposal;
    }

    @Override
    public List<LoanProposal> getLoanProposalsByStatus(LoanProposalStatus status) {
        List<LoanProposal> proposals = loanProposalRepository.findByStatus(status);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || canManageAllProposals(authentication)) {
            return proposals;
        }

        int currentUserId = currentUserId();
        return proposals.stream()
                .filter(proposal -> proposal.getIdUser() != null && proposal.getIdUser() == currentUserId)
                .toList();
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
    @AuditLog(action = AuditAction.CREATE, entityType = "Purchase")
    public Purchase acceptAsPurchase(int proposalId, Purchase finalPurchase) {
        LoanProposal proposal = getLoanProposalById(proposalId);
        requireNotYetHandled(proposal);

        Purchase createdPurchase = addPurchaseUseCase.addPurchase(finalPurchase);

        proposal.markAcceptedAsPurchase(createdPurchase.getId(), UserHelper.getCurrentUserId());
        loanProposalRepository.save(proposal);

        log.info("LoanProposal id={} accepted as purchase, created purchase id={}", proposalId, createdPurchase.getId());
        return createdPurchase;
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

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie jest
     * uprzywilejowany, a propozycja nie jest przypisana do niego (albo w ogole nie jest
     * przypisana - idUser == null, bo dopasowanie nadawcy sie nie powiodlo).
     */
    private void assertCanAccessProposal(LoanProposal proposal) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Brak kontekstu security (np. scheduler) - traktujemy jak pelny dostep
        if (authentication == null || canManageAllProposals(authentication)) {
            return;
        }

        Integer proposalUserId = proposal.getIdUser();
        if (proposalUserId == null || proposalUserId != currentUserId()) {
            throw new AccessDeniedException("Brak uprawnień do tej propozycji.");
        }
    }

    private int currentUserId() {
        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        return Math.toIntExact(user.getId());
    }

    /**
     * LoanProposal moze stac sie albo Loan, albo Purchase (albo obiema naraz do wyboru), wiec
     * uprawnienie "widze/zarzadzam wszystkimi propozycjami" pokrywa sie z uprawnieniami do
     * zarzadzania obiema tymi encjami docelowymi - nie ma osobnego FINANCE_LOAN_PROPOSAL_*.
     */
    private boolean canManageAllProposals(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_LOAN_READ_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_LOAN_WRITE_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PURCHASE_READ_ALL)
                        || grantedAuthority.getAuthority().equals(FINANCE_PURCHASE_WRITE_ALL));
    }
}
