package net.focik.homeoffice.finance.infrastructure.csvimport;

import tools.jackson.databind.ObjectMapper;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskError;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.finance.api.dto.BankCsvImportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankCsvImportAdapterTest {

    @Mock
    private BankCsvAsyncWorker bankCsvAsyncWorker;

    @Mock
    private AsyncTaskService asyncTaskService;

    private ObjectMapper objectMapper;
    private BankCsvImportAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new BankCsvImportAdapter(bankCsvAsyncWorker, asyncTaskService, objectMapper);
    }

    @Test
    void startImportAsync_ShouldCreateAsyncTaskAndReturnJobId_WhenValidInput() {
        byte[] csvContent = "test,csv,content".getBytes();
        int idUser = 123;

        AsyncTask mockTask = AsyncTask.builder()
                .jobId("job-123")
                .status(AsyncTaskStatus.QUEUED)
                .jobType("MILLENIUM_BANK_CSV_IMPORT")
                .build();

        when(asyncTaskService.startJob(0, "MILLENIUM_BANK_CSV_IMPORT")).thenReturn(mockTask);

        String jobId = adapter.startImportAsync(csvContent, idUser);

        assertThat(jobId).isEqualTo("job-123");
        verify(asyncTaskService).startJob(0, "MILLENIUM_BANK_CSV_IMPORT");
        verify(bankCsvAsyncWorker).processJobAsync("job-123", csvContent, idUser);
    }

    @Test
    void startImportAsync_ShouldReturnJobIdFromAsyncTask_WhenTaskCreated() {
        byte[] csvContent = "test,csv,content".getBytes();
        int idUser = 123;

        AsyncTask mockTask = AsyncTask.builder()
                .jobId("job-456")
                .status(AsyncTaskStatus.QUEUED)
                .jobType("MILLENIUM_BANK_CSV_IMPORT")
                .build();

        when(asyncTaskService.startJob(0, "MILLENIUM_BANK_CSV_IMPORT")).thenReturn(mockTask);

        String jobId = adapter.startImportAsync(csvContent, idUser);

        assertThat(jobId).isEqualTo("job-456");
    }

    @Test
    void getImportResult_ShouldReturnNullWhenTaskNotFound() {
        when(asyncTaskService.getJobStatus("invalid-job-id")).thenReturn(null);

        BankCsvImportResponse result = adapter.getImportResult("invalid-job-id");

        assertThat(result).isNull();
    }

    @Test
    void getImportResult_ShouldReturnNullWhenTaskStatusIsRunning() {
        String jobId = "job-123";
        AsyncTask runningTask = AsyncTask.builder()
                .jobId(jobId)
                .status(AsyncTaskStatus.RUNNING)
                .build();

        when(asyncTaskService.getJobStatus(jobId)).thenReturn(runningTask);

        BankCsvImportResponse result = adapter.getImportResult(jobId);

        assertThat(result).isNull();
    }

    @Test
    void getImportResult_ShouldReturnNullWhenTaskStatusIsQueued() {
        String jobId = "job-123";
        AsyncTask queuedTask = AsyncTask.builder()
                .jobId(jobId)
                .status(AsyncTaskStatus.QUEUED)
                .build();

        when(asyncTaskService.getJobStatus(jobId)).thenReturn(queuedTask);

        BankCsvImportResponse result = adapter.getImportResult(jobId);

        assertThat(result).isNull();
    }

    @Test
    void getImportResult_ShouldDeserializeResultWhenTaskSucceeded() throws Exception {
        String jobId = "job-123";
        BankCsvImportResponse expectedResult = BankCsvImportResponse.builder()
                .totalProcessed(10)
                .transactionCount(5)
                .purchaseCount(5)
                .duplicateCount(0)
                .transactions(new ArrayList<>())
                .purchases(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();

        String resultJson = objectMapper.writeValueAsString(expectedResult);

        AsyncTask succeededTask = AsyncTask.builder()
                .jobId(jobId)
                .status(AsyncTaskStatus.SUCCEEDED)
                .textractResultJson(resultJson)
                .build();

        when(asyncTaskService.getJobStatus(jobId)).thenReturn(succeededTask);

        BankCsvImportResponse result = adapter.getImportResult(jobId);

        assertThat(result).isNotNull();
        assertThat(result.getTotalProcessed()).isEqualTo(10);
        assertThat(result.getTransactionCount()).isEqualTo(5);
        assertThat(result.getPurchaseCount()).isEqualTo(5);
        assertThat(result.getDuplicateCount()).isEqualTo(0);
    }

    @Test
    void getImportResult_ShouldReturnErrorResponseWhenResultJsonIsEmpty() {
        String jobId = "job-123";
        AsyncTask task = AsyncTask.builder()
                .jobId(jobId)
                .status(AsyncTaskStatus.SUCCEEDED)
                .textractResultJson(null)
                .build();

        when(asyncTaskService.getJobStatus(jobId)).thenReturn(task);

        BankCsvImportResponse result = adapter.getImportResult(jobId);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).contains("No import data available");
    }

    @Test
    void getImportResult_ShouldReturnErrorResponseWhenDeserializationFails() {
        String jobId = "job-123";
        AsyncTask task = AsyncTask.builder()
                .jobId(jobId)
                .status(AsyncTaskStatus.SUCCEEDED)
                .textractResultJson("invalid json {{{")
                .build();

        when(asyncTaskService.getJobStatus(jobId)).thenReturn(task);

        BankCsvImportResponse result = adapter.getImportResult(jobId);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0)).contains("Failed to deserialize import result");
    }

    @Test
    void getImportResult_ShouldReturnErrorsWhenTaskFailed() throws Exception {
        String jobId = "job-123";

        // Create AsyncTaskError with message
        AsyncTaskError error = AsyncTaskError.builder()
                .message("Błąd przetwarzania")
                .build();

        AsyncTask failedTask = AsyncTask.builder()
                .jobId(jobId)
                .status(AsyncTaskStatus.FAILED)
                .errors(java.util.List.of(error))
                .build();

        when(asyncTaskService.getJobStatus(jobId)).thenReturn(failedTask);

        BankCsvImportResponse result = adapter.getImportResult(jobId);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).contains("Błąd przetwarzania");
    }
}
