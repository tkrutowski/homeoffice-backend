package net.focik.homeoffice.goahead.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.api.dto.CostDto;
import net.focik.homeoffice.goahead.api.mapper.ApiCostMapper;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.goahead.domain.cost.port.primary.AddCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.DeleteCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.UpdateCostUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
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

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/goahead/cost")
public class CostController extends ExceptionHandling {

    private final AddCostUseCase addCostUseCase;
    private final GetCostUseCase getCostUseCase;
    private final UpdateCostUseCase updateCostUseCase;
    private final DeleteCostUseCase deleteCostUseCase;
    private final ApiCostMapper mapper;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<CostDto> getCost(@PathVariable int id) {
        log.info("Request to get cost by id: {}", id);
        Cost cost = getCostUseCase.getCost(id);
        return ResponseEntity.ok(mapper.toDto(cost));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<List<CostDto>> getAllCosts() {
        log.info("Request to get all costs");
        List<Cost> costs = getCostUseCase.getAllCosts();
        List<CostDto> dtos = costs.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<Page<CostDto>> getCostsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "idCost") String sortField,
            @RequestParam(name = "direction", defaultValue = "DESC") String sortDirection,
            @RequestParam(name = "globalFilter", required = false) String globalFilter,
            @RequestParam(name = "idSeller", required = false) Integer idSeller,
            @RequestParam(name = "sellDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sellDate,
            @RequestParam(name = "dateComparisonType", required = false, defaultValue = "EQUALS") String dateComparisonType,
            @RequestParam(name = "amount", required = false) BigDecimal amount,
            @RequestParam(name = "amountComparisonType", required = false, defaultValue = "EQUALS") String amountComparisonType,
            @RequestParam(name = "status", required = false) PaymentStatus status
    ) {
        log.info("Request to get costs page with page: {}, size: {}, sort: {}, direction: {}, globalFilter: {}, idSeller: {}, date: {}, dateComparisonType: {}, amount: {}, amountComparisonType: {}, status: {}",
                page, size, sortField, sortDirection, globalFilter, idSeller, sellDate, dateComparisonType, amount, amountComparisonType, status);

        Page<Cost> costsPage = getCostUseCase.findCostsPageableWithFilters(
                page, size, sortField, sortDirection, globalFilter, idSeller,
                sellDate, dateComparisonType, amount, amountComparisonType, status);

        Page<CostDto> dtoPage = costsPage.map(mapper::toDto);

        log.debug("Found {} costs on page {} of {}",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getTotalPages());

        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<CostDto> addCost(@RequestBody CostDto costDto) {
        log.info("Request to add cost: {}", costDto);
        Cost cost = mapper.toDomain(costDto);
        Cost savedCost = addCostUseCase.addCost(cost);
        return new ResponseEntity<>(mapper.toDto(savedCost), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<CostDto> updateCost(@RequestBody CostDto costDto) {
        log.info("Request to update cost: {}", costDto);
        Cost cost = mapper.toDomain(costDto);
        Cost updatedCost = updateCostUseCase.updateCost(cost);
        return ResponseEntity.ok(mapper.toDto(updatedCost));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_DELETE_ALL')")
    public ResponseEntity<Void> deleteCost(@PathVariable int id) {
        log.info("Request to delete cost by id: {}", id);
        deleteCostUseCase.deleteCost(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ksef")
    public ResponseEntity<List<CostDto>> findKsefCosts(@RequestParam(name = "dateFrom") LocalDate fromDate, @RequestParam(name = "dateTo") LocalDate toDate )  {
        log.info("Request to find KSeF costs from {} to {}", fromDate, toDate);
        List<Cost> ksefInvoices = getCostUseCase.findKsefCosts(fromDate, toDate);
        log.info("Found {} KSeF costs", ksefInvoices.size());
        return new ResponseEntity<>(ksefInvoices.stream().map(mapper::toDto).toList(), HttpStatus.OK);
    }
}
