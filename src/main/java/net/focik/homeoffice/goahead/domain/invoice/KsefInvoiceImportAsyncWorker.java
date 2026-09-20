package net.focik.homeoffice.goahead.domain.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.audit.AsyncContext;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.ImportKsefInvoicesUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KsefInvoiceImportAsyncWorker {

    private final ImportKsefInvoicesUseCase importKsefInvoicesUseCase;
    private final AsyncTaskService asyncTaskService;

    @Async
    public void processJobAsync(String jobId, LocalDate fromDate, LocalDate toDate) {
        AsyncContext.setJobType("KSEF_INVOICE_IMPORT");
        try {
            AsyncTask job = asyncTaskService.getJobStatus(jobId);
            if (job == null) return;

            job.setStatus(AsyncTaskStatus.RUNNING);
            asyncTaskService.updateTask(job);

            log.info("Starting KSeF invoice import job: {} from {} to {}", jobId, fromDate, toDate);

            try {
                KsefInvoiceImportResult result = importKsefInvoicesUseCase.importKsefInvoices(fromDate, toDate);

                job.setTotal(result.found());
                job.setProcessed(result.imported().size());
                job.setDuplicates(result.duplicates());
                job.getErrors().addAll(result.errors());
                job.setMessage("Znaleziono: " + result.found() + ", Nowych: " + result.imported().size()
                        + ", Duplikatów: " + result.duplicates() + ", Pominiętych (korekty/waluta obca): " + result.skipped()
                        + ", Błędów: " + result.errors().size());

                if (result.errors().isEmpty()) {
                    job.setStatus(AsyncTaskStatus.SUCCEEDED);
                } else if (!result.imported().isEmpty()) {
                    job.setStatus(AsyncTaskStatus.PARTIAL);
                } else {
                    job.setStatus(AsyncTaskStatus.FAILED);
                }
            } catch (Exception e) {
                log.error("Error processing KSeF invoice import job {}", jobId, e);
                job.setStatus(AsyncTaskStatus.FAILED);
                job.setMessage("Wystąpił nieoczekiwany błąd podczas importu faktur: " + e.getMessage());
                job.getErrors().add(new AsyncTaskError(null, e.getMessage()));
            }

            asyncTaskService.updateTask(job);
            log.info("Finished KSeF invoice import job: {} with status: {}", jobId, job.getStatus());
        } finally {
            AsyncContext.clear();
        }
    }
}
