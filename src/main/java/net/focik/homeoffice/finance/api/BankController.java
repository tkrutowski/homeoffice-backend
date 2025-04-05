package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.BankDto;
import net.focik.homeoffice.finance.api.mapper.ApiBankMapper;
import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.domain.bank.port.primary.AddBankUseCase;
import net.focik.homeoffice.finance.domain.bank.port.primary.DeleteBankUseCase;
import net.focik.homeoffice.finance.domain.bank.port.primary.GetBankUseCase;
import net.focik.homeoffice.finance.domain.bank.port.primary.UpdateBankUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/bank")
//@CrossOrigin
public class BankController extends ExceptionHandling {

    private final ApiBankMapper mapper;
    private final AddBankUseCase addBankUseCase;
    private final UpdateBankUseCase updateBankUseCase;
    private final GetBankUseCase getBankUseCase;
    private final DeleteBankUseCase deleteBankUseCase;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<BankDto> getById(@PathVariable int id) {

        log.info("Try find bank by id: " + id);

        Bank bank = getBankUseCase.findById(id);

        log.info(bank != null ? "Found bank for id = " + id : "Not found bank for id = " + id);

        if (bank == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(mapper.toDto(bank), OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BankDto>> getAll() {
        log.info("Try get all banks");

        List<Bank> bankList = getBankUseCase.findByAll();

        log.info("Found " + bankList.size() + " banks.");

        return new ResponseEntity<>(bankList.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BankDto> addBank(@RequestBody BankDto bankDto) {
        log.info("Try add new bank.");

        Bank bank = mapper.toDomain(bankDto);
        Bank result = addBankUseCase.addBank(bank);

        log.info(result.getId() > 0 ? "Bank added with id = " + result : "No bank added!");

        if (result.getId() <= 0)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(mapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BankDto> updateBank(@RequestBody BankDto bankDto) {
        log.info("Try update bank with id: {}", bankDto.getId());

        Bank bank = updateBankUseCase.updateBank(mapper.toDomain(bankDto));
        return new ResponseEntity<>(mapper.toDto(bank), OK);
    }

    @DeleteMapping("/{idBank}")
    @PreAuthorize("hasAnyAuthority('FINANCE_DELETE_ALL', 'FINANCE_DELETE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteBank(@PathVariable int idBank) {
        log.info("Try delete bank with id: " + idBank);

        deleteBankUseCase.deleteBank(idBank);

        log.info("Deleted bank with id = " + idBank);

        return response(HttpStatus.NO_CONTENT, "Bank usunięty.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}
