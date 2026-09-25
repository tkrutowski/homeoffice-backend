package net.focik.homeoffice.logservice.infrastructure.live;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.focik.homeoffice.logservice.domain.model.LiveLogsResult;
import net.focik.homeoffice.logservice.domain.model.LogEntry;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.port.secondary.LiveLogSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Appender logbacka dopiety do root loggera, trzymajacy w pamieci procesu ostatnie N wpisow (bufor kolowy)
 * z rosnacym numerem sekwencyjnym. Nie blokuje logowania (krotka sekcja krytyczna, zero I/O) i nie zalezy od
 * plikow ani od S3, wiec dziala tak samo na kazdym profilu. Bufor jest pusty po restarcie aplikacji.
 */
@Component
public class LogbackLiveLogAdapter extends AppenderBase<ILoggingEvent> implements LiveLogSource {

    // logi samego endpointu odpytywania, inaczej kazdy poll dopisywalby kolejne wpisy do bufora
    private static final String EXCLUDED_LOGGER = "net.focik.homeoffice.logservice.api.LogsController";

    private final TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(36);
    private final int capacity;
    private final String instance;

    private final Object lock = new Object();
    private final ArrayDeque<Slot> buffer;
    private long lastSeq = 0;

    public LogbackLiveLogAdapter(@Value("${logs.live.buffer-size:2000}") int capacity,
                                 @Value("${APP_INSTANCE:unknown}") String instance) {
        this.capacity = Math.max(capacity, 1);
        this.instance = instance;
        this.buffer = new ArrayDeque<>(this.capacity);
    }

    @PostConstruct
    void attach() {
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
            setContext(context);
            setName("LIVE_LOGS");
            start();
            context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
        }
    }

    @PreDestroy
    void detach() {
        if (getContext() instanceof LoggerContext context) {
            context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(this);
        }
        stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (EXCLUDED_LOGGER.equals(event.getLoggerName())) {
            return;
        }
        LogEntry entry = new LogEntry(
                LocalDateTime.ofInstant(event.getInstant(), ZoneId.systemDefault()),
                event.getLevel().toString(),
                event.getThreadName(),
                abbreviator.abbreviate(event.getLoggerName()),
                message(event),
                instance);
        synchronized (lock) {
            if (buffer.size() == capacity) {
                buffer.removeFirst();
            }
            buffer.addLast(new Slot(++lastSeq, entry));
        }
    }

    @Override
    public LiveLogsResult entriesAfter(Long afterSeq, Set<LogLevel> levels, int limit) {
        List<Slot> snapshot;
        long latestSeq;
        synchronized (lock) {
            snapshot = new ArrayList<>(buffer);
            latestSeq = lastSeq;
        }
        long oldestAvailable = snapshot.isEmpty() ? latestSeq + 1 : snapshot.get(0).seq();

        // kursor z poprzedniego procesu (wiekszy niz aktualny numer) = restart aplikacji
        boolean restarted = afterSeq != null && afterSeq > latestSeq;
        if (afterSeq == null || restarted) {
            List<LogEntry> matching = snapshot.stream().map(Slot::entry).filter(e -> matches(e, levels)).toList();
            List<LogEntry> tail = matching.subList(Math.max(0, matching.size() - limit), matching.size());
            return new LiveLogsResult(instance, List.copyOf(tail), latestSeq, restarted, false);
        }

        boolean gap = afterSeq < oldestAvailable - 1;
        List<LogEntry> entries = new ArrayList<>();
        long cursor = latestSeq;
        boolean hasMore = false;
        for (Slot slot : snapshot) {
            if (slot.seq() <= afterSeq || !matches(slot.entry(), levels)) {
                continue;
            }
            if (entries.size() == limit) {
                hasMore = true;
                break;
            }
            entries.add(slot.entry());
            cursor = slot.seq();
        }
        // przy hasMore kursor stoi na ostatnim zwroconym wpisie; inaczej przesuwamy go na koniec bufora
        if (!hasMore) {
            cursor = latestSeq;
        }
        return new LiveLogsResult(instance, entries, cursor, gap, hasMore);
    }

    private static boolean matches(LogEntry entry, Set<LogLevel> levels) {
        return levels.isEmpty() || levels.contains(LogLevel.valueOf(entry.getLevel()));
    }

    private static String message(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable == null) {
            return message;
        }
        return message + "\n" + ThrowableProxyUtil.asString(throwable).replace("\r\n", "\n").stripTrailing();
    }

    private record Slot(long seq, LogEntry entry) {
    }
}
