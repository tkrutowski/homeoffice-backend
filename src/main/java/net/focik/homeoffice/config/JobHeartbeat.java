package net.focik.homeoffice.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * "Puls" zadan cyklicznych: gauge {@code homeoffice.job.last.run.timestamp} (Prometheus:
 * {@code homeoffice_job_last_run_timestamp_seconds{job}}) z czasem (epoch, sekundy) ostatniego uruchomienia zadania
 * przez scheduler. Pozwala zaalarmowac, gdy cron w ogole nie wystartowal (np. host byl wylaczony o 9:00) -
 * czego nie widac w zadnej innej metryce, bo "brak zdarzenia" nie zostawia sladu.
 * <p>
 * Seria powstaje dopiero przy pierwszym uruchomieniu po starcie aplikacji, wiec alert powinien patrzec na
 * {@code max_over_time(...)} z dluzszego okna (ostatnia znana wartosc przezywa restart), a nie na biezaca wartosc.
 * Wolaj tylko z metod {@code @Scheduled} (po sprawdzeniu, ze zadanie jest wlaczone), nie z recznych wyzwolen.
 */
@Component
public class JobHeartbeat {

    static final String METRIC = "homeoffice.job.last.run.timestamp";

    private final MeterRegistry registry;
    private final Clock clock;
    private final Map<String, AtomicLong> lastRun = new ConcurrentHashMap<>();

    @Autowired
    public JobHeartbeat(MeterRegistry registry) {
        this(registry, Clock.systemUTC());
    }

    JobHeartbeat(MeterRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
    }

    public void ran(String job) {
        lastRun.computeIfAbsent(job, j -> {
            AtomicLong holder = new AtomicLong();
            Gauge.builder(METRIC, holder, AtomicLong::get)
                    .tag("job", j)
                    .baseUnit("seconds")
                    .register(registry);
            return holder;
        }).set(clock.instant().getEpochSecond());
    }
}
