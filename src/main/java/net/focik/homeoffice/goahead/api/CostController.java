package net.focik.homeoffice.goahead.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.api.dto.BasicDto;
import net.focik.homeoffice.goahead.api.dto.CostDto;
import net.focik.homeoffice.goahead.api.mapper.ApiCostMapper;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.goahead.domain.cost.KsefCostJobService;
import net.focik.homeoffice.goahead.domain.cost.PdfCostJobService;
import net.focik.homeoffice.goahead.domain.cost.port.primary.AddCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.DeleteCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
import net.focik.homeoffice.goahead.domain.cost.port.primary.UpdateCostUseCase;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.FindKsefInvoiceRequest;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStartResponse;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final KsefCostJobService ksefCostJobService;
    private final PdfCostJobService pdfCostJobService;
    private final AsyncTaskService asyncTaskService;
    private final ObjectMapper objectMapper;

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
            @RequestParam(name = "idSupplier", required = false) Integer idSupplier,
            @RequestParam(name = "sellDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sellDate,
            @RequestParam(name = "dateComparisonType", required = false, defaultValue = "EQUALS") String dateComparisonType,
            @RequestParam(name = "amount", required = false) BigDecimal amount,
            @RequestParam(name = "amountComparisonType", required = false, defaultValue = "EQUALS") String amountComparisonType,
            @RequestParam(name = "status", required = false) PaymentStatus status
    ) {
        log.info("Request to get costs page with page: {}, size: {}, sort: {}, direction: {}, globalFilter: {}, idSupplier: {}, date: {}, dateComparisonType: {}, amount: {}, amountComparisonType: {}, status: {}",
                page, size, sortField, sortDirection, globalFilter, idSupplier, sellDate, dateComparisonType, amount, amountComparisonType, status);

        Page<Cost> costsPage = getCostUseCase.findCostsPageableWithFilters(
                page, size, sortField, sortDirection, globalFilter, idSupplier,
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

    @PostMapping("/pdf")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<AsyncTaskStartResponse> generateAndSavePdfById(@RequestBody List<Integer> costIds) {
        log.info("Request to generate PDF and save to S3 for {} costs", costIds.size());
        
        String jobId = pdfCostJobService.startJob(costIds);
        
        return new ResponseEntity<>(new AsyncTaskStartResponse(jobId), HttpStatus.ACCEPTED);
    }
    
    @GetMapping("/pdf/jobs/{jobId}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<AsyncTask> getPdfJobStatus(@PathVariable String jobId) {
        log.info("Request to get PDF job status for jobId: {}", jobId);
        
        AsyncTask jobStatus = pdfCostJobService.getJobStatus(jobId);
        
        if (jobStatus == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(jobStatus, HttpStatus.OK);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<CostDto> updateCost(@RequestBody CostDto costDto) {
        log.info("Request to update cost: {}", costDto);
        Cost cost = mapper.toDomain(costDto);
        Cost updatedCost = updateCostUseCase.updateCost(cost);
        return ResponseEntity.ok(mapper.toDto(updatedCost));
    }

    @PutMapping("/paymentstatus/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public void updatePaymentStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Request to update payment status for cost with id: {}", id);
        updateCostUseCase.updatePaymentStatus(id, PaymentStatus.valueOf(basicDto.getValue()));
        log.info("Payment status updated successfully for cost with id: {} to status: {}", id, basicDto.getValue());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_DELETE_ALL')")
    public ResponseEntity<Void> deleteCost(@PathVariable int id) {
        log.info("Request to delete cost by id: {}", id);
        deleteCostUseCase.deleteCost(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ksef")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<AsyncTaskStartResponse> findKsefCosts(@RequestBody FindKsefInvoiceRequest request )  {
        log.info("Request to start job to find KSeF costs from {} to {}", request.fromDate(), request.toDate());
        
        String jobId = ksefCostJobService.startJob(request.fromDate(), request.toDate());
        
        return new ResponseEntity<>(new AsyncTaskStartResponse(jobId), HttpStatus.ACCEPTED);
    }
    
    @GetMapping("/ksef/jobs/{jobId}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<AsyncTask> getKsefCostJobStatus(@PathVariable String jobId) {
        log.info("Request to get KSeF cost job status for jobId: {}", jobId);
        
        AsyncTask jobStatus = ksefCostJobService.getJobStatus(jobId);
        
        if (jobStatus == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(jobStatus, HttpStatus.OK);
    }

    @GetMapping("/upload/jobs/{jobId}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<AsyncTask> getUploadJobStatus(@PathVariable String jobId) {
        log.info("Pobieranie statusu uploadu: jobId={}", jobId);
        AsyncTask jobStatus = asyncTaskService.getJobStatus(jobId);
        if (jobStatus == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(jobStatus, HttpStatus.OK);
    }

    @GetMapping("/upload/jobs/{jobId}/result")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    public ResponseEntity<CostDto> getUploadJobResult(@PathVariable String jobId) {
        log.info("Pobieranie wyniku parsowania: jobId={}", jobId);
        AsyncTask jobStatus = asyncTaskService.getJobStatus(jobId);

        if (jobStatus == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (jobStatus.getTextractResultJson() == null || jobStatus.getTextractResultJson().isEmpty()) {
            log.warn("Brak wyników parsowania dla jobId {}", jobId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        try {
            // Deserializacja JSON z AsyncTask do CostDto (JSON zawiera CostDto, nie Cost)
            CostDto costDto = objectMapper.readValue(jobStatus.getTextractResultJson(), CostDto.class);
            log.info("Zwrócony sparsowany cost dla jobId {}", jobId);
            return ResponseEntity.ok(costDto);
        } catch (Exception e) {
            log.error("Błąd deserializacji wyniku parsowania dla jobId {}: {}", jobId, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

