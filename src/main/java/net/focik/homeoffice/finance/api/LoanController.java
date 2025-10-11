package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.BasicDto;
import net.focik.homeoffice.finance.api.dto.LoanDto;
import net.focik.homeoffice.finance.api.dto.LoanInstallmentDto;
import net.focik.homeoffice.finance.api.mapper.ApiLoanMapper;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.domain.loan.port.primary.AddLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.DeleteLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.GetLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.UpdateLoanUseCase;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/loan")
@CrossOrigin
class LoanController {

    public static final String MAPPED_DOMAIN_OBJECT_TO_LOAN_DTO = "Mapped domain object to Loan DTO: {}";
    public static final String MAPPED_LOAN_DTO_TO_DOMAIN_OBJECT = "Mapped Loan DTO to domain object: {}";
    public static final String MAPPED_DOMAIN_OBJECT_TO_LOAN_INSTALLMENT_DTO = "Mapped domain object to LoanInstallment DTO: {}";
    public static final String MAPPED_LOAN_INSTALLMENT_DTO_TO_DOMAIN_OBJECT = "Mapped LoanInstallment DTO to domain object: {}";
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
        log.info("Request to get loans with status: {}.", loanStatus);

        List<Loan> loans = getLoanUseCase.getLoansByStatus(loanStatus, installment);
        log.info("Found {} loans.", loans.size());

        return new ResponseEntity<>(loans.stream()
                .peek(loan -> log.debug("Found loan {}", loan))
                .map(apiLoanMapper::toDto)
                .peek(dto -> log.debug("Mapped found loan {}", dto))
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @GetMapping("/{idLoan}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<LoanDto> getLoanById(@PathVariable int idLoan) {
        log.info("Request to get loan by id: {}", idLoan);

        Loan loan = getLoanUseCase.getLoanById(idLoan, true);
        if (loan == null) {
            log.warn("No loan found with id: {}", idLoan);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found loan {}", loan);
        LoanDto dto = apiLoanMapper.toDto(loan);
        log.info("Mapped to loan DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/page")
    ResponseEntity<Page<LoanDto>> getLoansPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "date") String sortField,
            @RequestParam(name = "direction", defaultValue = "DESC") String sortDirection,
            @RequestParam(name = "globalFilter", required = false) String globalFilter,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "bankName", required = false) String bankName,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "dateComparisonType", required = false, defaultValue = "EQUALS") String dateComparisonType,
            @RequestParam(name = "amount", required = false) BigDecimal amount,
            @RequestParam(name = "amountComparisonType", required = false, defaultValue = "EQUALS") String amountComparisonType,
            @RequestParam(name = "status", required = false) PaymentStatus status
    ) {
        log.info("Request to get loans page with page: {}, size: {}, sort: {}, direction: {}, globalFilter: {}, name: {}, bankName: {}, date: {}, dateComparisonType: {}, amount: {}, amountComparisonType: {}, status: {}",
                page, size, sortField, sortDirection, globalFilter, name, bankName, date, dateComparisonType, amount, amountComparisonType, status);

        Page<Loan> loansPage = getLoanUseCase.findLoansPageableWithFilters(
                page, size, sortField, sortDirection, globalFilter, name, bankName,
                date, dateComparisonType, amount, amountComparisonType, status);

        Page<LoanDto> dtoPage = loansPage.map(apiLoanMapper::toDto);

        log.debug("Found {} loans on page {} of {}",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getTotalPages());

        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanDto> addLoan(@RequestBody LoanDto loanDto) {
        log.info("Request to add a new loan received with data: {}", loanDto);

        Loan loanToAdd = apiLoanMapper.toDomain(loanDto);
        log.info(MAPPED_LOAN_DTO_TO_DOMAIN_OBJECT, loanToAdd);

        Loan result = addLoanUseCase.addLoan(loanToAdd);
        log.info("Loan added successfully: {}", result);

        LoanDto dto = apiLoanMapper.toDto(result);
        log.info(MAPPED_DOMAIN_OBJECT_TO_LOAN_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanDto> updateLoan(@RequestBody LoanDto loanDto) {
        log.info("Request to edit a loan received with data: {}", loanDto);

        Loan loan = apiLoanMapper.toDomain(loanDto);
        log.info(MAPPED_LOAN_DTO_TO_DOMAIN_OBJECT, loan);

        Loan result = updateLoanUseCase.updateLoan(loan);
        log.info("Loan updated successfully: {}", result);

        LoanDto dto = apiLoanMapper.toDto(result);
        log.info(MAPPED_DOMAIN_OBJECT_TO_LOAN_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanDto> updateLoanStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Request to edit a loan status received with data: {}", basicDto);

        Loan result = updateLoanUseCase.updateLoanStatus(id, PaymentStatus.valueOf(basicDto.getValue()));
        log.debug("Loan updated: {}", result);

        LoanDto dto = apiLoanMapper.toDto(result);
        log.info(MAPPED_DOMAIN_OBJECT_TO_LOAN_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @DeleteMapping("/{idLoan}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void deleteLoan(@PathVariable int idLoan) {
        log.info("Request to delete a loan received with id: {}", idLoan);
        deleteLoanUseCase.deleteLoanById(idLoan);
        log.info("Loan deleted successfully with id: {}", idLoan);
    }

    //-----------------------------------------------------------------------------------------------------------
    //LOAN INSTALLMENT
    //
    @GetMapping("/installment/{idLoan}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<LoanInstallmentDto>> getLoanInstallmentsByLoan(@PathVariable int idLoan) {

        log.info("Request to get loan installments by loan id: {}", idLoan);

        List<LoanInstallment> loanInstallments = getLoanUseCase.getLoanInstallments(idLoan);
        log.info("Found {} loan installments.", loanInstallments.size());

        return new ResponseEntity<>(loanInstallments.stream()
                .peek(loanInstallment -> log.debug("Found loan installment {}", loanInstallment))
                .map(apiLoanMapper::toDto)
                .peek(dto -> log.debug("Mapped found loan installment {}", dto))
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @PostMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanInstallmentDto> addLoanInstallment(@RequestBody LoanInstallmentDto loanInstallmentDto) {
        log.info("Request to add a new LoanInstallment with data: {}", loanInstallmentDto);

        LoanInstallment loanInstallmentToAdd = apiLoanMapper.toDomain(loanInstallmentDto);
        log.debug(MAPPED_LOAN_INSTALLMENT_DTO_TO_DOMAIN_OBJECT, loanInstallmentToAdd);

        LoanInstallment result = addLoanUseCase.addLoanInstallment(loanInstallmentToAdd);
        log.debug("Loan installment added: {}", result);

        LoanInstallmentDto dto = apiLoanMapper.toDto(result);
        log.debug(MAPPED_DOMAIN_OBJECT_TO_LOAN_INSTALLMENT_DTO, dto);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<LoanInstallmentDto> updateLoanInstallment(@RequestBody LoanInstallmentDto loanInstallmentDto) {
        log.info("Request to edit a loan installment with data: {}", loanInstallmentDto);

        LoanInstallment loanInstallment = apiLoanMapper.toDomain(loanInstallmentDto);
        log.debug(MAPPED_LOAN_INSTALLMENT_DTO_TO_DOMAIN_OBJECT, loanInstallment);

        LoanInstallment result = updateLoanUseCase.updateLoanInstallment(loanInstallment);
        log.debug("Loan installment updated: {}", result);

        LoanInstallmentDto dto = apiLoanMapper.toDto(result);
        log.debug(MAPPED_DOMAIN_OBJECT_TO_LOAN_INSTALLMENT_DTO, dto);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @DeleteMapping("/installment/{idLoanInstallment}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void deleteLoanInstallment(@PathVariable int idLoanInstallment) {
        log.info("Request to delete fee installment with id: {}", idLoanInstallment);
        deleteLoanUseCase.deleteLoanInstallmentById(idLoanInstallment);
        log.info("Fee Installment with id: {} deleted successfully", idLoanInstallment);
    }
}