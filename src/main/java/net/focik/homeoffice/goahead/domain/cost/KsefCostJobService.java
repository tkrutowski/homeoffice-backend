package net.focik.homeoffice.goahead.domain.cost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KsefCostJobService {

    public static final String JOB_TYPE = "KSEF_FETCH_COSTS";

    private final KsefCostAsyncWorker ksefCostAsyncWorker;
    private final AsyncTaskService asyncTaskService;

    public String startJob(LocalDate fromDate, LocalDate toDate) {
        // We pass 0 as total initially because we don't know how many costs will be fetched
        AsyncTask task = asyncTaskService.startJob(0, JOB_TYPE);

        ksefCostAsyncWorker.processJobAsync(task.getJobId(), fromDate, toDate);

        return task.getJobId();
    }

    public AsyncTask getJobStatus(String jobId) {
        return asyncTaskService.getJobStatus(jobId);
    }
}