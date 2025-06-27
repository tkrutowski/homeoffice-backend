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

    public static final String MAPPED_BANK_DTO_TO_DOMAIN_OBJECT = "Mapped BankDTO to domain object: {}";
    public static final String MAPPED_TO_BANK_DTO = "Mapped to Bank DTO: {}";
    private final ApiBankMapper mapper;
    private final AddBankUseCase addBankUseCase;
    private final UpdateBankUseCase updateBankUseCase;
    private final GetBankUseCase getBankUseCase;
    private final DeleteBankUseCase deleteBankUseCase;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<BankDto> getById(@PathVariable int id) {
        log.info("Request to get bank by id: {}", id);
        Bank bank = getBankUseCase.findById(id);

        if (bank == null) {
            log.warn("No bank found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Bank found: {}", bank);
        BankDto bankDto = mapper.toDto(bank);
        log.debug(MAPPED_TO_BANK_DTO, bankDto);
        return new ResponseEntity<>(bankDto, OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BankDto>> getAll() {
        log.info("Request to get all banks");
        List<Bank> bankList = getBankUseCase.findByAll();

        if (bankList.isEmpty()) {
            log.warn("No banks found.");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.info("Found {} banks.", bankList.size());

        return new ResponseEntity<>(bankList.stream()
                .peek(bank -> log.debug("Found bank {}", bank))
                .map(mapper::toDto)
                .peek(dto -> log.debug("Mapped found bank {}", dto))
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BankDto> addBank(@RequestBody BankDto bankDto) {
        log.info("Request to add a new bank received with data: {}", bankDto);

        Bank bankToAdd = mapper.toDomain(bankDto);
        log.debug(MAPPED_BANK_DTO_TO_DOMAIN_OBJECT, bankToAdd);

        Bank result = addBankUseCase.addBank(bankToAdd);
        log.info("Bank added successfully: {}", result);

        BankDto dto = mapper.toDto(result);
        log.debug(MAPPED_TO_BANK_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BankDto> updateBank(@RequestBody BankDto bankDto) {
        log.info("Request to edit a bank received with data: {}", bankDto);

        Bank bankToUpdate = mapper.toDomain(bankDto);
        log.debug(MAPPED_BANK_DTO_TO_DOMAIN_OBJECT, bankToUpdate);

        Bank bank = updateBankUseCase.updateBank(bankToUpdate);
        log.info("Bank updated successfully: {}", bank);

        BankDto dto = mapper.toDto(bank);
        log.debug(MAPPED_TO_BANK_DTO, dto);

        return new ResponseEntity<>(dto, OK);
    }

    @DeleteMapping("/{idBank}")
    @PreAuthorize("hasAnyAuthority('FINANCE_DELETE_ALL', 'FINANCE_DELETE') or hasRole('ROLE_ADMIN')")
    public void deleteBank(@PathVariable int idBank) {
        log.info("Request to delete bank with id: {}", idBank);
        deleteBankUseCase.deleteBank(idBank);
        log.info("Bank with id: {} deleted successfully", idBank);
    }

}
