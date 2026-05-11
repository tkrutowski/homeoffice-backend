package net.focik.homeoffice.goahead.domain.cost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfCostJobService {

    public static final String JOB_TYPE = "PDF_GENERATE_COST";

    private final PdfCostAsyncWorker pdfCostAsyncWorker;
    private final AsyncTaskService asyncTaskService;

    public String startJob(List<Integer> costIds) {
        AsyncTask task = asyncTaskService.startJob(costIds.size(), JOB_TYPE);

        pdfCostAsyncWorker.processJobAsync(task.getJobId(), costIds);

        return task.getJobId();
    }

    public AsyncTask getJobStatus(String jobId) {
        return asyncTaskService.getJobStatus(jobId);
    }
}