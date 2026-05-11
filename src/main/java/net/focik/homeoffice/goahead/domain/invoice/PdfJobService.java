package net.focik.homeoffice.goahead.domain.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfJobService {

    public static final String JOB_TYPE = "PDF_GENERATE";

    private final PdfAsyncWorker pdfAsyncWorker;
    private final AsyncTaskService asyncTaskService;

    public String startJob(List<Integer> invoiceIds) {
        AsyncTask task = asyncTaskService.startJob(invoiceIds.size(), JOB_TYPE);

        pdfAsyncWorker.processJobAsync(task.getJobId(), invoiceIds);

        return task.getJobId();
    }

    public AsyncTask getJobStatus(String jobId) {
        return asyncTaskService.getJobStatus(jobId);
    }
}
