package net.focik.homeoffice.goahead.domain.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.audit.AsyncContext;
import net.focik.homeoffice.goahead.api.dto.ZusDraDataDto;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.GetInvoiceUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZusDraAsyncWorker {

    private final GetInvoiceUseCase getInvoiceUseCase;
    private final GetCostUseCase getCostUseCase;
    private final AsyncTaskService asyncTaskService;
    private final ObjectMapper objectMapper;
    private final net.focik.homeoffice.goahead.domain.cost.KsefCostJobService ksefCostJobService;

    @Async
    public void processJobAsync(String jobId, LocalDate settlementDate) {
        AsyncContext.setJobType("ZUS_DRA_PREPARE");
        try {
            AsyncTask job = asyncTaskService.getJobStatus(jobId);
            if (job == null) return;

            job.setStatus(AsyncTaskStatus.RUNNING);
            asyncTaskService.updateTask(job);

            log.info("Starting ZUS DRA data preparation job: {}", jobId);

            try {
                YearMonth previousMonth = YearMonth.from(settlementDate).minusMonths(1);
                LocalDate from = previousMonth.atDay(1);
                LocalDate to = previousMonth.atEndOfMonth();

                log.debug("Calculating totals for period: {} to {}", from, to);

                List<Invoice> invoices = getInvoiceUseCase.findBySellDateBetween(from, to);
                BigDecimal totalIncome = invoices.stream()
                        .map(i -> i.getAmountSum().getNumber().numberValue(BigDecimal.class))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                log.debug("Total income for period {}: {}", previousMonth, totalIncome);

                LocalDateTime ksefCheckSince = settlementDate.minusDays(1).atStartOfDay();
                boolean ksefFetched = asyncTaskService.hasSucceededJobSince(
                        net.focik.homeoffice.goahead.domain.cost.KsefCostJobService.JOB_TYPE, ksefCheckSince);

                log.debug("KSeF costs already fetched: {}", ksefFetched);

                if (!ksefFetched) {
                    log.info("KSeF costs not fetched, importing from KSeF for period {} to {}", from, to);
                    getCostUseCase.findKsefCosts(from, to);
                }

                List<net.focik.homeoffice.goahead.domain.cost.Cost> costs = getCostUseCase.findBySellDateBetween(from, to);
                BigDecimal totalCosts = costs.stream()
                        .map(c -> c.getAmountSum().getNumber().numberValue(BigDecimal.class))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                log.debug("Total costs for period {}: {}", previousMonth, totalCosts);

                ZusDraDataDto result = ZusDraDataDto.builder()
                        .period(previousMonth.toString())
                        .totalIncome(totalIncome)
                        .totalCosts(totalCosts)
                        .build();

                String resultJson = objectMapper.writeValueAsString(result);
                job.setTextractResultJson(resultJson);
                job.setStatus(AsyncTaskStatus.SUCCEEDED);
                job.setMessage("Dane ZUS DRA przygotowane");
                job.setProcessed(1);
                job.setTotal(1);

            } catch (Exception e) {
                log.error("Error processing ZUS DRA job {}", jobId, e);
                job.setStatus(AsyncTaskStatus.FAILED);
                job.setMessage("Błąd podczas przygotowania danych ZUS DRA: " + e.getMessage());
                job.getErrors().add(new AsyncTaskError(null, e.getMessage()));
            }

            asyncTaskService.updateTask(job);
            log.info("Finished ZUS DRA job: {} with status: {}", jobId, job.getStatus());
        } finally {
            AsyncContext.clear();
        }
    }
}
