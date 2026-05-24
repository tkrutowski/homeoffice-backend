package net.focik.homeoffice.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AsyncTaskRepository asyncTaskRepository;

    public AsyncTask startJob(int totalItems, String jobType) {
        String jobId = UUID.randomUUID().toString();

        AsyncTask task = AsyncTask.builder()
                .jobId(jobId)
                .status(AsyncTaskStatus.QUEUED)
                .jobType(jobType)
                .total(totalItems)
                .processed(0)
                .errors(new ArrayList<>())
                .build();

        return asyncTaskRepository.save(task);
    }

    public AsyncTask getJobStatus(String jobId) {
        return asyncTaskRepository.findById(jobId).orElse(null);
    }

    public AsyncTask updateTask(AsyncTask task) {
        return asyncTaskRepository.save(task);
    }

    public void updateTaskStatus(String jobId, AsyncTaskStatus status) {
        AsyncTask task = getJobStatus(jobId);
        if (task != null) {
            task.setStatus(status);
            updateTask(task);
        }
    }

    public boolean hasSucceededJobSince(String jobType, LocalDateTime since) {
        return asyncTaskRepository.existsByJobTypeAndStatusAndCreatedAtGreaterThanEqual(
                jobType, AsyncTaskStatus.SUCCEEDED, since);
    }
}