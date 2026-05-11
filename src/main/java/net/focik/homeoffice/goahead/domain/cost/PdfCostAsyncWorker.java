package net.focik.homeoffice.goahead.domain.cost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.cost.port.primary.GetCostUseCase;
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
public class PdfCostAsyncWorker {

    private final GetCostUseCase getCostUseCase;
    private final AsyncTaskService asyncTaskService;

    @Async
    public void processJobAsync(String jobId, List<Integer> costIds) {
        AsyncTask job = asyncTaskService.getJobStatus(jobId);
        if (job == null) return;

        job.setStatus(AsyncTaskStatus.RUNNING);
        asyncTaskService.updateTask(job);

        log.info("Starting PDF generation job: {} for {} costs", jobId, costIds.size());

        int processed = 0;
        int failed = 0;

        for (Integer id : costIds) {
            try {
                String s3Url = getCostUseCase.generateAndSendCostToS3(id);
                if (s3Url == null) {
                    failed++;
                    job.getErrors().add(new AsyncTaskError(String.valueOf(id), "Nie udało się wygenerować PDF dla kosztu"));
                } else {
                    processed++;
                }

                // Update progress after each cost
                job.setProcessed(processed + failed);
                asyncTaskService.updateTask(job);
            } catch (Exception e) {
                log.error("Error generating PDF for cost {}", id, e);
                failed++;
                job.getErrors().add(new AsyncTaskError(String.valueOf(id), e.getMessage()));
                job.setProcessed(processed + failed);
                asyncTaskService.updateTask(job);
            }
        }

        if (failed > 0) {
            if (processed > 0) {
                job.setStatus(AsyncTaskStatus.PARTIAL);
                job.setMessage("Część plików PDF została wygenerowana z błędami.");
            } else {
                job.setStatus(AsyncTaskStatus.FAILED);
                job.setMessage("Błąd generowania wszystkich plików PDF.");
            }
        } else {
            job.setStatus(AsyncTaskStatus.SUCCEEDED);
            job.setMessage("Pomyślnie wygenerowano wszystkie pliki PDF.");
        }

        asyncTaskService.updateTask(job);
        log.info("Finished PDF generation job: {} with status: {}", jobId, job.getStatus());
    }
}