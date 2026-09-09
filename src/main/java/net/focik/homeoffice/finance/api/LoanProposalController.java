package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.LoanDto;
import net.focik.homeoffice.finance.api.dto.LoanProposalDto;
import net.focik.homeoffice.finance.api.dto.PurchaseDto;
import net.focik.homeoffice.finance.api.mapper.ApiLoanMapper;
import net.focik.homeoffice.finance.api.mapper.ApiLoanProposalMapper;
import net.focik.homeoffice.finance.api.mapper.ApiPurchaseMapper;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.AcceptLoanProposalAsPurchaseUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.AcceptLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.DeleteLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.GetLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.primary.IgnoreLoanProposalUseCase;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/loan-proposal")
@CrossOrigin
class LoanProposalController {

    private final GetLoanProposalUseCase getLoanProposalUseCase;
    private final AcceptLoanProposalUseCase acceptLoanProposalUseCase;
    private final AcceptLoanProposalAsPurchaseUseCase acceptLoanProposalAsPurchaseUseCase;
    private final IgnoreLoanProposalUseCase ignoreLoanProposalUseCase;
    private final DeleteLoanProposalUseCase deleteLoanProposalUseCase;
    private final ApiLoanProposalMapper apiLoanProposalMapper;
    private final ApiLoanMapper apiLoanMapper;
    private final ApiPurchaseMapper apiPurchaseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<LoanProposalDto>> getLoanProposalsByStatus(
            @RequestParam(value = "status", defaultValue = "EXTRACTED") LoanProposalStatus status) {
        log.info("Request to get loan proposals with status: {}", status);

        List<LoanProposal> proposals = getLoanProposalUseCase.getLoanProposalsByStatus(status);
        log.info("Found {} loan proposals.", proposals.size());

        return ResponseEntity.ok(proposals.stream()
                .map(apiLoanProposalMapper::toDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<LoanProposalDto> getLoanProposalById(@PathVariable int id) {
        log.info("Request to get loan proposal by id: {}", id);

        LoanProposal proposal = getLoanProposalUseCase.getLoanProposalById(id);
        return ResponseEntity.ok(apiLoanProposalMapper.toDto(proposal));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<LoanDto> accept(@PathVariable int id, @RequestBody LoanDto loanDto) {
        log.info("Request to accept loan proposal id={} with data: {}", id, loanDto);

        Loan finalLoan = apiLoanMapper.toDomain(loanDto);
        Loan createdLoan = acceptLoanProposalUseCase.accept(id, finalLoan);

        log.info("Loan proposal id={} accepted, created loan id={}", id, createdLoan.getId());
        return new ResponseEntity<>(apiLoanMapper.toDto(createdLoan), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/accept-as-purchase")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<PurchaseDto> acceptAsPurchase(@PathVariable int id, @RequestBody PurchaseDto purchaseDto) {
        log.info("Request to accept loan proposal id={} as purchase with data: {}", id, purchaseDto);

        Purchase finalPurchase = apiPurchaseMapper.toDomain(purchaseDto);
        Purchase createdPurchase = acceptLoanProposalAsPurchaseUseCase.acceptAsPurchase(id, finalPurchase);

        log.info("Loan proposal id={} accepted as purchase, created purchase id={}", id, createdPurchase.getId());
        return new ResponseEntity<>(apiPurchaseMapper.toDto(createdPurchase), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/ignore")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<LoanProposalDto> ignore(@PathVariable int id) {
        log.info("Request to ignore loan proposal id={}", id);

        LoanProposal proposal = ignoreLoanProposalUseCase.ignore(id);
        return ResponseEntity.ok(apiLoanProposalMapper.toDto(proposal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    void delete(@PathVariable int id) {
        log.info("Request to delete loan proposal id={}", id);
        deleteLoanProposalUseCase.deleteLoanProposalById(id);
    }
}
