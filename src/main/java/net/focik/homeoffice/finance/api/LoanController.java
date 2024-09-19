package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.BasicDto;
import net.focik.homeoffice.finance.api.dto.LoanDto;
import net.focik.homeoffice.finance.api.dto.LoanInstallmentDto;
import net.focik.homeoffice.finance.api.mapper.ApiLoanMapper;
import net.focik.homeoffice.finance.domain.exception.LoanNotValidException;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.domain.loan.port.primary.AddLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.DeleteLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.GetLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.UpdateLoanUseCase;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/loan")
@CrossOrigin
class LoanController {

    private final GetLoanUseCase getLoanUseCase;
    private final AddLoanUseCase addLoanUseCase;
    private final UpdateLoanUseCase updateLoanUseCase;
    private final DeleteLoanUseCase deleteLoanUseCase;
    private final ApiLoanMapper apiLoanMapper;


    //
    //LOAN
    //
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<LoanDto>> getLoansByStatus(@RequestParam(value = "status") PaymentStatus loanStatus,
                                                   @RequestParam(value = "installment", defaultValue = "false") boolean installment) {

        log.info("Get loans with status: " + loanStatus);

        List<Loan> loans = getLoanUseCase.getLoansByStatus(loanStatus, installment);

        return new ResponseEntity<>(loans.stream()
                .map(apiLoanMapper::toDto)
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @GetMapping("/{idLoan}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<LoanDto> getLoanById(@PathVariable int idLoan) {

        log.info("Get loan by id: " + idLoan);

        Loan loan = getLoanUseCase.getLoanById(idLoan, true);

        return new ResponseEntity<>(apiLoanMapper.toDto(loan), HttpStatus.OK);
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanDto> addLoan(@RequestBody LoanDto loanDto) {
        log.info("Try add new loan.");

        Loan loan = apiLoanMapper.toDomain(loanDto);
        Loan result = addLoanUseCase.addLoan(loan);

        log.info("Loan added with id = " + result.getId());

        return ResponseEntity.ok(apiLoanMapper.toDto(result));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanDto> updateLoan(@RequestBody LoanDto loanDto) {
        log.info("Try update loan.");

        Loan loan = apiLoanMapper.toDomain(loanDto);
        Loan result = updateLoanUseCase.updateLoan(loan);

        log.info("Loan updated with id = " + result.getId());

        return new ResponseEntity<>(apiLoanMapper.toDto(result), HttpStatus.OK);
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanDto> updateLoanStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Try update employment status.");

        Loan result = updateLoanUseCase.updateLoanStatus(id, PaymentStatus.valueOf(basicDto.getValue()));
        return new ResponseEntity<>(apiLoanMapper.toDto(result), HttpStatus.OK);
    }

    @DeleteMapping("/{idLoan}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteLoan(@PathVariable int idLoan) {
        log.info("Try to delete loan with id: " + idLoan);

        deleteLoanUseCase.deleteLoanById(idLoan);

        log.info("Deleted loan with id = " + idLoan);

        return response(HttpStatus.NO_CONTENT, "Pożyczka usunięta.");
    }

    //-----------------------------------------------------------------------------------------------------------
    //LOAN INSTALLMENT
    //
    @GetMapping("/installment/{idLoan}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<LoanInstallmentDto>> getLoanInstallmentsByLoan(@PathVariable int idLoan) {

        log.info("Get all loan installment by loan id: " + idLoan);

        List<LoanInstallment> loanInstallments = getLoanUseCase.getLoanInstallments(idLoan);

        return new ResponseEntity<>(loanInstallments.stream()
                .map(apiLoanMapper::toDto)
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @PostMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanInstallmentDto> addLoanInstallment(@RequestBody LoanInstallmentDto loanInstallmentDto) {
        log.info("Try add new loan installment.");

        LoanInstallment loanInstallment = apiLoanMapper.toDomain(loanInstallmentDto);
        LoanInstallment result = addLoanUseCase.addLoanInstallment(loanInstallment);

        log.info("Loan installment added with id = " + result.getIdLoanInstallment());

        return new ResponseEntity<>(apiLoanMapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanInstallmentDto> updateLoanInstallment(@RequestBody LoanInstallmentDto loanInstallmentDto) {
        log.info("Try update loan installment.");

        LoanInstallment loanInstallment = apiLoanMapper.toDomain(loanInstallmentDto);
        LoanInstallment result = updateLoanUseCase.updateLoanInstallment(loanInstallment);

        log.info("Loan installment updated with id = " + result.getIdLoanInstallment());

        return new ResponseEntity<>(apiLoanMapper.toDto(result), HttpStatus.CREATED);
    }

    @DeleteMapping("/installment/{idLoanInstallment}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteLoanInstallment(@PathVariable int idLoanInstallment) {
        log.info("Try to delete loan installment with id: " + idLoanInstallment);

        deleteLoanUseCase.deleteLoanInstallmentById(idLoanInstallment);

        log.info("Deleted loan with id = " + idLoanInstallment);

        return response(HttpStatus.NO_CONTENT, "Pożyczka usunięta.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}