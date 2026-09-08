package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.IngestLoanEmailRequest;
import net.focik.homeoffice.finance.api.mapper.ApiLoanProposalMapper;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.RawLoanEmail;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.IngestLoanProposalUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter wejściowy dla n8n. Poza JWT-em - dostęp kontroluje InternalTokenAuthenticationFilter
 * (nagłówek X-Internal-Token) i to, że nginx w ogóle nie przepuszcza /internal/** z internetu
 * (mapowany jest tylko /api/** i /actuator/**, patrz nginx.conf).
 */
@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/internal/loan-proposals")
class LoanProposalIngestController {

    private final IngestLoanProposalUseCase ingestLoanProposalUseCase;
    private final ApiLoanProposalMapper apiLoanProposalMapper;

    @PostMapping("/ingest")
    ResponseEntity<Void> ingest(@RequestBody IngestLoanEmailRequest request) {
        log.info("Ingest request received from n8n, messageId={}, from={}, subject={}",
                request.getMessageId(), request.getFrom(), request.getSubject());

        if (request.getMessageId() == null || request.getMessageId().isBlank()) {
            log.warn("Ingest request rejected: missing messageId");
            return ResponseEntity.badRequest().build();
        }

        RawLoanEmail email = apiLoanProposalMapper.toDomain(request);
        LoanProposal proposal = ingestLoanProposalUseCase.ingest(email);

        log.info("Loan proposal id={} accepted for processing", proposal.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
