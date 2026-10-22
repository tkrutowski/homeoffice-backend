package net.focik.homeoffice.config;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JobHeartbeatTest {

    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    private double value(String job) {
        return registry.get(JobHeartbeat.METRIC).tag("job", job).gauge().value();
    }

    @Test
    @DisplayName("should expose the time of the last run per job and update it on the next run")
    void ran_ShouldExposeAndUpdateTimestamp() {
        JobHeartbeat first = new JobHeartbeat(registry, Clock.fixed(Instant.ofEpochSecond(1_000), ZoneOffset.UTC));
        first.ran("payment_reminder");
        first.ran("purchase_report_weekly");
        assertThat(value("payment_reminder")).isEqualTo(1_000);
        assertThat(value("purchase_report_weekly")).isEqualTo(1_000);

        // ten sam obiekt, pozniejszy zegar - symulacja kolejnego przebiegu zadania
        JobHeartbeat later = new JobHeartbeat(registry, Clock.fixed(Instant.ofEpochSecond(5_000), ZoneOffset.UTC));
        later.ran("brand_new_job");
        assertThat(value("brand_new_job")).isEqualTo(5_000);
        assertThat(value("payment_reminder")).isEqualTo(1_000);
    }

    @Test
    @DisplayName("should publish a Prometheus series named homeoffice_job_last_run_timestamp_seconds")
    void ran_ShouldUseExpectedPrometheusName() {
        new JobHeartbeat(registry).ran("payment_reminder");

        assertThat(registry.scrape()).contains("homeoffice_job_last_run_timestamp_seconds{")
                .contains("job=\"payment_reminder\"");
    }

    @Test
    @DisplayName("should be created by Spring (two constructors - the one for Spring must be picked explicitly)")
    void shouldBeInstantiableBySpring() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean(io.micrometer.core.instrument.MeterRegistry.class, () -> registry);
            context.register(JobHeartbeat.class);
            context.refresh();

            context.getBean(JobHeartbeat.class).ran("payment_reminder");

            assertThat(value("payment_reminder")).isPositive();
        }
    }
}
