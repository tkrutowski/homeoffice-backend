package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.BankTransactionDto;
import net.focik.homeoffice.finance.api.mapper.ApiBankTransactionMapper;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.port.primary.AddBankTransactionUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.DeleteBankTransactionUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GetBankTransactionUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.UpdateBankTransactionUseCase;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/bank-transaction")
public class BankTransactionController extends ExceptionHandling {

    private final ApiBankTransactionMapper mapper;
    private final AddBankTransactionUseCase addBankTransactionUseCase;
    private final UpdateBankTransactionUseCase updateBankTransactionUseCase;
    private final GetBankTransactionUseCase getBankTransactionUseCase;
    private final DeleteBankTransactionUseCase deleteBankTransactionUseCase;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<BankTransactionDto> getById(@PathVariable int id) {
        log.info("Request to get bank transaction by id: {}", id);

        BankTransaction bankTransaction = getBankTransactionUseCase.findById(id);

        if (bankTransaction == null) {
            log.warn("No bank transaction found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Bank transaction found: {}", bankTransaction);
        BankTransactionDto dto = mapper.toDto(bankTransaction);
        log.info("Mapped to BankTransaction DTO found: {}", dto);
        return new ResponseEntity<>(dto, OK);
    }

    @GetMapping("/between")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<BankTransactionDto>> findBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        log.info("Request to get bank transactions between {} and {}", dateFrom, dateTo);

        int idUser = Math.toIntExact(UserHelper.getUser().getId());
        List<BankTransaction> bankTransactions = getBankTransactionUseCase.findBetween(dateFrom, dateTo, idUser);
        log.info("Found {} bank transactions.", bankTransactions.size());

        List<BankTransactionDto> dtos = bankTransactions.stream()
                .map(mapper::toDto)
                .toList();

        return new ResponseEntity<>(dtos, OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<BankTransactionDto> addBankTransaction(@RequestBody BankTransactionDto bankTransactionDto) {
        log.info("Try add new bank transaction.");

        BankTransaction bankTransaction = mapper.toDomain(bankTransactionDto);
        BankTransaction result = addBankTransactionUseCase.addBankTransaction(bankTransaction);

        log.info(result.getId() > 0 ? "bank transaction added with id = " + result : "No bank transaction added!");

        if (result.getId() <= 0)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(mapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<BankTransactionDto> updateBankTransaction(@RequestBody BankTransactionDto bankTransactionDto) {
        log.info("Try update bank transaction with id: {}", bankTransactionDto.getId());

        BankTransaction bankTransaction = updateBankTransactionUseCase.updateBankTransaction(mapper.toDomain(bankTransactionDto));
        return new ResponseEntity<>(mapper.toDto(bankTransaction), OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void deleteBankTransaction(@PathVariable int id) {
        log.info("Try delete bank transaction with id: {}", id);

        deleteBankTransactionUseCase.deleteBankTransaction(id);

        log.info("Deleted bank transaction with id = {}", id);
    }
}
