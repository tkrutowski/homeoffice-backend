package net.focik.homeoffice.goahead.domain.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KsefInvoiceImportJobService {

    public static final String JOB_TYPE = "KSEF_IMPORT_INVOICES";

    private final KsefInvoiceImportAsyncWorker worker;
    private final AsyncTaskService asyncTaskService;

    public String startJob(LocalDate fromDate, LocalDate toDate) {
        // liczba faktur nie jest znana przed zapytaniem do KSeF
        AsyncTask task = asyncTaskService.startJob(0, JOB_TYPE);

        worker.processJobAsync(task.getJobId(), fromDate, toDate);

        return task.getJobId();
    }

    public AsyncTask getJobStatus(String jobId) {
        return asyncTaskService.getJobStatus(jobId);
    }
}
