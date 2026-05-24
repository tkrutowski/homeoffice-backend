package net.focik.homeoffice.goahead.domain.cost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.audit.AsyncContext;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KsefCostAsyncWorker {

    private final GetCostUseCase getCostUseCase;
    private final AsyncTaskService asyncTaskService;

    @Async
    public void processJobAsync(String jobId, LocalDate fromDate, LocalDate toDate) {
        AsyncContext.setJobType("KSEF_COST_IMPORT");
        try {
            AsyncTask job = asyncTaskService.getJobStatus(jobId);
            if (job == null) return;

            job.setStatus(AsyncTaskStatus.RUNNING);
            asyncTaskService.updateTask(job);

            log.info("Starting KseF cost fetch job: {} from {} to {}", jobId, fromDate, toDate);

            try {
                KsefImportResult result = getCostUseCase.findKsefCosts(fromDate, toDate);

                job.setTotal(result.found());
                job.setProcessed(result.newCosts().size());
                job.setDuplicates(result.duplicates());
                job.setStatus(AsyncTaskStatus.SUCCEEDED);
                job.setMessage("Znaleziono: " + result.found() + ", Nowych: " + result.newCosts().size() + ", Duplikatów: " + result.duplicates());

            } catch (Exception e) {
                log.error("Error processing KSeF cost fetch job {}", jobId, e);
                job.setStatus(AsyncTaskStatus.FAILED);
                job.setMessage("Wystąpił nieoczekiwany błąd podczas pobierania kosztów: " + e.getMessage());
                job.getErrors().add(new AsyncTaskError(null, e.getMessage()));
            }

            asyncTaskService.updateTask(job);
            log.info("Finished KseF cost fetch job: {} with status: {}", jobId, job.getStatus());
        } finally {
            AsyncContext.clear();
        }
    }
}