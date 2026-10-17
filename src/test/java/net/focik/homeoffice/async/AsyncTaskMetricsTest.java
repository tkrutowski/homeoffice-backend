package net.focik.homeoffice.async;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncTaskMetricsTest {

    private SimpleMeterRegistry registry;
    private AsyncTaskMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AsyncTaskMetrics(registry);
    }

    private AsyncTask task(String jobId, AsyncTaskStatus status) {
        return AsyncTask.builder()
                .jobId(jobId)
                .jobType("KSEF_INVOICE_SEND")
                .status(status)
                .createdAt(LocalDateTime.now().minusSeconds(3))
                .build();
    }

    private double count(String status) {
        var counter = registry.find("homeoffice.async.tasks")
                .tags("job_type", "KSEF_INVOICE_SEND", "status", status).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    @DisplayName("should count a finished task once and record its duration")
    void recordIfFinished_ShouldCountOnce_WhenCalledRepeatedlyForSameJob() {
        AsyncTask task = task("job-1", AsyncTaskStatus.FAILED);

        metrics.recordIfFinished(task);
        metrics.recordIfFinished(task);

        assertThat(count("FAILED")).isEqualTo(1);
        assertThat(Objects.requireNonNull(registry.find("homeoffice.async.tasks.duration").timer()).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should ignore tasks that are not finished yet")
    void recordIfFinished_ShouldIgnore_WhenStatusIsQueuedOrRunning() {
        metrics.recordIfFinished(task("job-2", AsyncTaskStatus.QUEUED));
        metrics.recordIfFinished(task("job-3", AsyncTaskStatus.RUNNING));

        assertThat(registry.find("homeoffice.async.tasks").counter()).isNull();
    }

    @Test
    @DisplayName("should count each final status separately")
    void recordIfFinished_ShouldSplitByStatus() {
        metrics.recordIfFinished(task("job-4", AsyncTaskStatus.SUCCEEDED));
        metrics.recordIfFinished(task("job-5", AsyncTaskStatus.PARTIAL));

        assertThat(count("SUCCEEDED")).isEqualTo(1);
        assertThat(count("PARTIAL")).isEqualTo(1);
    }
}
