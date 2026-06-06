package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.TransactionCategoryDto;
import net.focik.homeoffice.finance.api.mapper.ApiTransactionCategoryMapper;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
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
@RequestMapping("/api/v1/finance/transaction-category")
public class TransactionCategoryController extends ExceptionHandling {

    private final AddTransactionCategoryUseCase addTransactionCategoryUseCase;
    private final UpdateTransactionCategoryUseCase updateTransactionCategoryUseCase;
    private final GetTransactionCategoryUseCase getTransactionCategoryUseCase;
    private final DeleteTransactionCategoryUseCase deleteTransactionCategoryUseCase;
    private final ApiTransactionCategoryMapper apiMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<TransactionCategoryDto>> getAll() {
        log.info("Request to get all transaction categories");

        List<TransactionCategoryDto> categories = getTransactionCategoryUseCase.getAllTransactionCategories().stream()
                .map(apiMapper::toDto)
                .toList();

        log.info("Found {} transaction categories", categories.size());
        return new ResponseEntity<>(categories, OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<TransactionCategoryDto> getById(@PathVariable int id) {
        log.info("Request to get transaction category by id: {}", id);

        try {
            TransactionCategory category = getTransactionCategoryUseCase.getTransactionCategoryById(id);
            return new ResponseEntity<>(apiMapper.toDto(category), OK);
        } catch (IllegalArgumentException e) {
            log.warn("Transaction category with id {} not found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<TransactionCategoryDto> add(@RequestBody TransactionCategoryDto categoryDto) {
        log.info("Try add new transaction category");

        TransactionCategory domain = apiMapper.toDomain(categoryDto);
        TransactionCategory saved = addTransactionCategoryUseCase.addTransactionCategory(domain);

        log.info("Transaction category added with id = {}", saved.getId());
        return new ResponseEntity<>(apiMapper.toDto(saved), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<TransactionCategoryDto> update(@RequestBody TransactionCategoryDto categoryDto) {
        log.info("Try update transaction category with id: {}", categoryDto.getId());

        try {
            TransactionCategory domain = apiMapper.toDomain(categoryDto);
            TransactionCategory updated = updateTransactionCategoryUseCase.updateTransactionCategory(domain);
            return new ResponseEntity<>(apiMapper.toDto(updated), OK);
        } catch (IllegalArgumentException e) {
            log.warn("Transaction category with id {} not found", categoryDto.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void delete(@PathVariable int id) {
        log.info("Try delete transaction category with id: {}", id);

        deleteTransactionCategoryUseCase.deleteTransactionCategory(id);
        log.info("Deleted transaction category with id = {}", id);
    }
}
