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
public class ZusDraJobService {

    public static final String JOB_TYPE = "ZUS_DRA_PREPARE";

    private final ZusDraAsyncWorker zusDraAsyncWorker;
    private final AsyncTaskService asyncTaskService;

    public String startJob(LocalDate settlementDate) {
        AsyncTask task = asyncTaskService.startJob(1, JOB_TYPE);

        zusDraAsyncWorker.processJobAsync(task.getJobId(), settlementDate);

        return task.getJobId();
    }

    public AsyncTask getJobStatus(String jobId) {
        return asyncTaskService.getJobStatus(jobId);
    }
}
