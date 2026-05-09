package net.focik.homeoffice.goahead.domain.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.invoice.ksef.model.SendKsefInvoiceInfoResponse;
import net.focik.homeoffice.goahead.domain.invoice.port.primary.UpdateInvoiceUseCase;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KsefAsyncWorker {

    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final AsyncTaskService asyncTaskService;

    @Async
    public void processJobAsync(String jobId, List<Integer> invoiceIds) {
        AsyncTask job = asyncTaskService.getJobStatus(jobId);
        if (job == null) return;

        job.setStatus(AsyncTaskStatus.RUNNING);
        asyncTaskService.updateTask(job);
        
        log.info("Starting KseF job: {} with {} invoices", jobId, invoiceIds.size());

        try {
            SendKsefInvoiceInfoResponse response = updateInvoiceUseCase.sendInvoicesToKsef(invoiceIds);
            
            job.setProcessed(response.invoiceCount());
            
            if (response.failedInvoiceCount() > 0) {
                if (response.successInvoiceCount() > 0) {
                    job.setStatus(AsyncTaskStatus.PARTIAL);
                    job.setMessage("Część faktur została wysłana z błędem.");
                } else {
                    job.setStatus(AsyncTaskStatus.FAILED);
                    job.setMessage("Błąd wysyłki wszystkich faktur.");
                }
            } else {
                job.setStatus(AsyncTaskStatus.SUCCEEDED);
                job.setMessage("Pomyślnie wysłano wszystkie faktury.");
            }
            
        } catch (Exception e) {
            log.error("Error processing KSeF job {}", jobId, e);
            job.setStatus(AsyncTaskStatus.FAILED);
            job.setMessage("Wystąpił nieoczekiwany błąd podczas wysyłki: " + e.getMessage());
            job.getErrors().add(new AsyncTaskError(null, e.getMessage()));
        }
        
        asyncTaskService.updateTask(job);
        log.info("Finished KseF job: {} with status: {}", jobId, job.getStatus());
    }
}
