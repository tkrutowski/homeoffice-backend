package net.focik.homeoffice.finance.infrastructure.csvimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.finance.api.dto.BankCsvImportResponse;
import net.focik.homeoffice.finance.domain.csvimport.port.primary.ParseBankCsvUseCase;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class BankCsvImportAdapter implements ParseBankCsvUseCase {

    private final BankCsvAsyncWorker bankCsvAsyncWorker;
    private final AsyncTaskService asyncTaskService;
    private final ObjectMapper objectMapper;

    @Override
    public String startImportAsync(byte[] fileContent, int idUser) {
        AsyncTask asyncTask = asyncTaskService.startJob(0, "MILLENIUM_BANK_CSV_IMPORT");
        String jobId = asyncTask.getJobId();

        log.info("Created async task {} for CSV import", jobId);

        bankCsvAsyncWorker.processJobAsync(jobId, fileContent, idUser);

        return jobId;
    }

    @Override
    public BankCsvImportResponse getImportResult(String jobId) {
        AsyncTask asyncTask = asyncTaskService.getJobStatus(jobId);

        if (asyncTask == null) {
            log.warn("Async task {} not found", jobId);
            return null;
        }

        if (asyncTask.getStatus() == AsyncTaskStatus.RUNNING || asyncTask.getStatus() == AsyncTaskStatus.QUEUED) {
            return null;
        }

        if (asyncTask.getStatus() == AsyncTaskStatus.FAILED || asyncTask.getStatus() == AsyncTaskStatus.PARTIAL) {
            log.warn("Import job {} failed with status {}", jobId, asyncTask.getStatus());
            List<String> errors = asyncTask.getErrors() != null
                    ? asyncTask.getErrors().stream()
                    .map(AsyncTaskError::getMessage)
                    .toList()
                    : List.of(asyncTask.getMessage() != null ? asyncTask.getMessage() : "Import failed");
            return BankCsvImportResponse.builder()
                    .totalProcessed(asyncTask.getProcessed())
                    .transactionCount(0)
                    .purchaseCount(0)
                    .duplicateCount(asyncTask.getDuplicates())
                    .errors(errors)
                    .build();
        }

        if (asyncTask.getTextractResultJson() == null || asyncTask.getTextractResultJson().isEmpty()) {
            log.warn("No result JSON in async task {}", jobId);
            return BankCsvImportResponse.builder()
                    .totalProcessed(asyncTask.getProcessed())
                    .transactionCount(0)
                    .purchaseCount(0)
                    .duplicateCount(asyncTask.getDuplicates())
                    .errors(List.of(asyncTask.getMessage() != null ? asyncTask.getMessage() : "No import data available"))
                    .build();
        }

        try {
            return objectMapper.readValue(asyncTask.getTextractResultJson(), BankCsvImportResponse.class);
        } catch (Exception e) {
            log.error("Error deserializing result JSON from task {}", jobId, e);
            return BankCsvImportResponse.builder()
                    .totalProcessed(asyncTask.getProcessed())
                    .transactionCount(0)
                    .purchaseCount(0)
                    .duplicateCount(asyncTask.getDuplicates())
                    .errors(List.of("Failed to deserialize import result: " + e.getMessage()))
                    .build();
        }
    }
}
