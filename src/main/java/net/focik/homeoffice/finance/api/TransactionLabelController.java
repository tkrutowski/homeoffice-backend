package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.TransactionLabelDto;
import net.focik.homeoffice.finance.api.mapper.ApiTransactionLabelMapper;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.port.primary.*;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/transaction-label")
public class TransactionLabelController extends ExceptionHandling {

    private final AddTransactionLabelUseCase addTransactionLabelUseCase;
    private final UpdateTransactionLabelUseCase updateTransactionLabelUseCase;
    private final GetTransactionLabelUseCase getTransactionLabelUseCase;
    private final DeleteTransactionLabelUseCase deleteTransactionLabelUseCase;
    private final ApiTransactionLabelMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<TransactionLabelDto>> getAll() {
        log.info("Request to get all transaction labels");

        List<TransactionLabelDto> labels = getTransactionLabelUseCase.getAllTransactionLabels().stream()
                .map(apiMapper::toDto)
                .toList();

        log.info("Found {} transaction labels", labels.size());
        return new ResponseEntity<>(labels, OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<TransactionLabelDto> getById(@PathVariable int id) {
        log.info("Request to get transaction label by id: {}", id);

        try {
            TransactionLabel label = getTransactionLabelUseCase.getTransactionLabelById(id);
            return new ResponseEntity<>(apiMapper.toDto(label), OK);
        } catch (IllegalArgumentException e) {
            log.warn("Transaction label with id {} not found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<TransactionLabelDto> add(@RequestBody TransactionLabelDto labelDto) {
        log.info("Try add new transaction label");

        TransactionLabel domain = apiMapper.toDomain(labelDto);
        TransactionLabel saved = addTransactionLabelUseCase.addTransactionLabel(domain);

        log.info("Transaction label added with id = {}", saved.getId());
        return new ResponseEntity<>(apiMapper.toDto(saved), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<TransactionLabelDto> update(@RequestBody TransactionLabelDto labelDto) {
        log.info("Try update transaction label with id: {}", labelDto.getId());

        try {
            TransactionLabel domain = apiMapper.toDomain(labelDto);
            TransactionLabel updated = updateTransactionLabelUseCase.updateTransactionLabel(domain);
            return new ResponseEntity<>(apiMapper.toDto(updated), OK);
        } catch (IllegalArgumentException e) {
            log.warn("Transaction label with id {} not found", labelDto.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void delete(@PathVariable int id) {
        log.info("Try delete transaction label with id: {}", id);

        deleteTransactionLabelUseCase.deleteTransactionLabel(id);
        log.info("Deleted transaction label with id = {}", id);
    }
}
