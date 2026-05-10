package net.focik.homeoffice.goahead.domain.cost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KsefCostAsyncWorker {

    private final GetCostUseCase getCostUseCase;
    private final AsyncTaskService asyncTaskService;

    @Async
    public void processJobAsync(String jobId, LocalDate fromDate, LocalDate toDate) {
        AsyncTask job = asyncTaskService.getJobStatus(jobId);
        if (job == null) return;

        job.setStatus(AsyncTaskStatus.RUNNING);
        asyncTaskService.updateTask(job);

        log.info("Starting KseF cost fetch job: {} from {} to {}", jobId, fromDate, toDate);

        try {
            List<Cost> costs = getCostUseCase.findKsefCosts(fromDate, toDate);

            job.setProcessed(costs.size());
            job.setTotal(costs.size());
            job.setStatus(AsyncTaskStatus.SUCCEEDED);
            job.setMessage("Pomyślnie pobrano koszty z KSeF. Liczba: " + costs.size());

        } catch (Exception e) {
            log.error("Error processing KSeF cost fetch job {}", jobId, e);
            job.setStatus(AsyncTaskStatus.FAILED);
            job.setMessage("Wystąpił nieoczekiwany błąd podczas pobierania kosztów: " + e.getMessage());
            job.getErrors().add(new AsyncTaskError(null, e.getMessage()));
        }

        asyncTaskService.updateTask(job);
        log.info("Finished KseF cost fetch job: {} with status: {}", jobId, job.getStatus());
    }
}