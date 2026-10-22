package net.focik.homeoffice.goahead.domain.invoice.ksef;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.focik.homeoffice.goahead.domain.exception.KsefResponseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.akmf.ksef.sdk.client.model.ApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KsefMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final KsefMetrics metrics = new KsefMetrics(registry);

    private long count(String operation, String outcome) {
        var timer = registry.find(KsefMetrics.METRIC).tag("operation", operation).tag("outcome", outcome).timer();
        return timer == null ? 0 : timer.count();
    }

    @Test
    @DisplayName("should return the result and count a success")
    void record_ShouldCountSuccess() {
        String result = metrics.record("login", () -> "token");

        assertThat(result).isEqualTo("token");
        assertThat(count("login", "success")).isEqualTo(1);
    }

    @Test
    @DisplayName("should count HTTP 401 from KSeF as unauthorized, even when wrapped, and rethrow the same exception")
    void record_ShouldCountUnauthorized_WhenApiExceptionIs401() {
        KsefResponseException wrapped = new KsefResponseException("Failed to login to KSeF system with token",
                new ApiException(401, "Unauthorized"));

        assertThatThrownBy(() -> metrics.record("login", () -> {
            throw wrapped;
        })).isSameAs(wrapped);

        assertThat(count("login", "unauthorized")).isEqualTo(1);
        assertThat(count("login", "success")).isZero();
    }

    @Test
    @DisplayName("should count a rejected authentication process (status >= 400) as auth_rejected")
    void record_ShouldCountAuthRejected_WhenAuthStatusCodeSet() {
        assertThatThrownBy(() -> metrics.record("login", () -> {
            throw new KsefResponseException("Błąd uwierzytelnienia KSeF (kod 450)", 450);
        })).isInstanceOf(KsefResponseException.class);

        assertThat(count("login", "auth_rejected")).isEqualTo(1);
    }

    @Test
    @DisplayName("should count other failures as error")
    void record_ShouldCountError_WhenOtherFailure() {
        assertThatThrownBy(() -> metrics.record("find_invoices", () -> {
            throw new KsefResponseException("boom", new ApiException(500, "Server error"));
        })).isInstanceOf(KsefResponseException.class);

        assertThat(count("find_invoices", "error")).isEqualTo(1);
        assertThat(count("find_invoices", "unauthorized")).isZero();
    }

    private double invoices(String result) {
        var counter = registry.find(KsefMetrics.INVOICES_METRIC).tag("result", result).counter();
        return counter == null ? -1 : counter.count();
    }

    @Test
    @DisplayName("should count accepted and rejected invoices of a sending session")
    void recordInvoices_ShouldCountAcceptedAndRejected() {
        metrics.recordInvoices(3, 1);
        metrics.recordInvoices(2, 0);

        assertThat(invoices("accepted")).isEqualTo(5);
        assertThat(invoices("rejected")).isEqualTo(1);
    }

    @Test
    @DisplayName("should treat missing counts as zero but still register both series")
    void recordInvoices_ShouldRegisterSeries_WhenCountsMissing() {
        metrics.recordInvoices(null, null);

        assertThat(invoices("accepted")).isZero();
        assertThat(invoices("rejected")).isZero();
    }
}
