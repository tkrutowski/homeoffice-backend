package net.focik.homeoffice.goahead.domain.invoice.ksef;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.focik.homeoffice.goahead.domain.exception.KsefResponseException;
import org.springframework.stereotype.Component;
import pl.akmf.ksef.sdk.client.model.ApiException;

import java.util.function.Supplier;

/**
 * Metryka {@code homeoffice.ksef.operations} (timer: liczba i czas) z tagami {@code operation}
 * ({@code login}, {@code find_invoices}, {@code send_invoices}) i {@code outcome}:
 * <ul>
 *   <li>{@code success},</li>
 *   <li>{@code unauthorized} - KSeF odpowiedzial HTTP 401 (typowo wygasly {@code KSEF_TOKEN}),</li>
 *   <li>{@code auth_rejected} - KSeF odrzucil uwierzytelnienie (status procesu logowania &gt;= 400),</li>
 *   <li>{@code error} - cokolwiek innego.</li>
 * </ul>
 * Logowanie (tokenem lub certyfikatem - ten drugi w trybie testowym KSeF) jest liczone tylko, gdy faktycznie dochodzi do polaczenia z KSeF (nie przy uzyciu tokenu z cache).
 * Operacje biznesowe, ktore wolaja logowanie, dziedzicza jego wynik (blad logowania liczy sie w obu).
 * <p>
 * Osobno {@code homeoffice.ksef.invoices{result}} ({@code accepted}/{@code rejected}) liczy faktury wg statusu sesji
 * wysylkowej: operacja {@code send_invoices} moze sie udac, a KSeF i tak odrzucic czesc faktur.
 */
@Component
public class KsefMetrics {

    static final String METRIC = "homeoffice.ksef.operations";
    static final String INVOICES_METRIC = "homeoffice.ksef.invoices";

    private final MeterRegistry registry;

    public KsefMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public <T> T record(String operation, Supplier<T> action) {
        long start = System.nanoTime();
        String outcome = "success";
        try {
            return action.get();
        } catch (RuntimeException e) {
            outcome = outcomeOf(e);
            throw e;
        } finally {
            Timer.builder(METRIC)
                    .tag("operation", operation)
                    .tag("outcome", outcome)
                    .register(registry)
                    .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    /** Faktury z jednej sesji wysylkowej wg statusu sesji KSeF; {@code null} oznacza brak danych (liczone jako 0). */
    public void recordInvoices(Integer accepted, Integer rejected) {
        increment("accepted", accepted);
        increment("rejected", rejected);
    }

    private void increment(String result, Integer amount) {
        // liczniki tworzymy od razu (takze z zerem), zeby seria istniala, zanim cokolwiek zostanie odrzucone
        var counter = registry.counter(INVOICES_METRIC, "result", result);
        if (amount != null && amount > 0) {
            counter.increment(amount);
        }
    }

    static String outcomeOf(Throwable error) {
        // lancuch przyczyn z limitem, na wypadek cyklu
        Throwable current = error;
        for (int depth = 0; current != null && depth < 10; depth++, current = current.getCause()) {
            if (current instanceof ApiException api && api.getCode() == 401) {
                return "unauthorized";
            }
            if (current instanceof KsefResponseException ksef && ksef.getAuthStatusCode() != null) {
                return "auth_rejected";
            }
        }
        return "error";
    }
}
