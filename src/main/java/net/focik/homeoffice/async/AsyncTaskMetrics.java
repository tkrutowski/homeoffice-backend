package net.focik.homeoffice.async;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Metryki zadan {@link AsyncTask}: {@code homeoffice.async.tasks} (licznik zakonczonych zadan wg typu i statusu)
 * i {@code homeoffice.async.tasks.duration} (czas od utworzenia do zakonczenia).
 * <p>
 * Workery wolaja {@code updateTask} wielokrotnie, takze po ustawieniu statusu koncowego, wiec zakonczenie
 * danego jobId liczymy tylko raz (pamiec ograniczona do ostatnich {@value #MAX_REMEMBERED} zadan).
 */
@Component
public class AsyncTaskMetrics {

    private static final int MAX_REMEMBERED = 1000;

    private final MeterRegistry registry;

    private final Set<String> recordedJobIds = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_REMEMBERED;
                }
            }));

    public AsyncTaskMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordIfFinished(AsyncTask task) {
        if (task == null || !isFinal(task.getStatus()) || !recordedJobIds.add(task.getJobId())) {
            return;
        }

        String jobType = task.getJobType() == null ? "unknown" : task.getJobType();
        String status = task.getStatus().name();

        registry.counter("homeoffice.async.tasks", "job_type", jobType, "status", status).increment();

        if (task.getCreatedAt() != null) {
            Timer.builder("homeoffice.async.tasks.duration")
                    .tag("job_type", jobType)
                    .tag("status", status)
                    .register(registry)
                    .record(Duration.between(task.getCreatedAt(), LocalDateTime.now()));
        }
    }

    private static boolean isFinal(AsyncTaskStatus status) {
        return status == AsyncTaskStatus.SUCCEEDED
                || status == AsyncTaskStatus.FAILED
                || status == AsyncTaskStatus.PARTIAL;
    }
}
