package net.focik.homeoffice.logservice.domain;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogLevelOverride;
import net.focik.homeoffice.logservice.domain.model.LogLevelsInfo;
import net.focik.homeoffice.logservice.domain.model.LoggerInfo;
import net.focik.homeoffice.logservice.domain.port.primary.ManageLogLevelsUseCase;
import net.focik.homeoffice.logservice.domain.port.secondary.LogLevelControl;
import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;
import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Tymczasowe zmiany poziomów logow z automatycznym powrotem. Stan trzymany jest w pamięci procesu, wiec po restarcie
 * aplikacji wszystko wraca do konfiguracji domyślnej.
 */
@Slf4j
@Service
class LogLevelsService implements ManageLogLevelsUseCase {

    private static final Pattern LOGGER_NAME = Pattern.compile("[A-Za-z0-9_$.]+");

    private final LogLevelControl control;
    private final List<String> allowedPrefixes;
    private final int defaultTtlMinutes;
    private final int maxTtlMinutes;
    private final String instance;
    private final Clock clock;
    private final ScheduledExecutorService scheduler;

    // chronione monitorem tego obiektu
    private final Map<String, ActiveOverride> active = new HashMap<>();

    @Autowired
    LogLevelsService(LogLevelControl control,
                     @Value("#{'${logs.levels.allowed-prefixes:net.focik.homeoffice,org.springframework.security,org.hibernate.SQL,software.amazon.awssdk}'.split(',')}") List<String> allowedPrefixes,
                     @Value("${logs.levels.default-ttl-minutes:15}") int defaultTtlMinutes,
                     @Value("${logs.levels.max-ttl-minutes:1440}") int maxTtlMinutes,
                     @Value("${APP_INSTANCE:unknown}") String instance) {
        this(control, allowedPrefixes, defaultTtlMinutes, maxTtlMinutes, instance, Clock.systemDefaultZone(),
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "log-level-revert");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    LogLevelsService(LogLevelControl control, List<String> allowedPrefixes, int defaultTtlMinutes, int maxTtlMinutes,
                     String instance, Clock clock, ScheduledExecutorService scheduler) {
        this.control = control;
        this.allowedPrefixes = allowedPrefixes.stream().map(String::trim).filter(prefix -> !prefix.isEmpty()).toList();
        this.defaultTtlMinutes = defaultTtlMinutes;
        this.maxTtlMinutes = maxTtlMinutes;
        this.instance = instance;
        this.clock = clock;
        this.scheduler = scheduler;
    }

    @Override
    public synchronized LogLevelsInfo getLevels() {
        List<LogLevelOverride> overrides = active.values().stream()
                .map(ActiveOverride::override)
                .sorted(Comparator.comparing(LogLevelOverride::logger))
                .toList();
        return new LogLevelsInfo(instance, control.getRootLevel(), allowedPrefixes, defaultTtlMinutes, maxTtlMinutes, overrides);
    }

    @Override
    public List<LoggerInfo> getLoggers(String prefix) {
        String filter = prefix == null ? "" : prefix.trim();
        return control.getLoggers().stream()
                .filter(info -> allowedPrefixes.stream().anyMatch(allowed -> isUnder(info.name(), allowed)))
                .filter(info -> filter.isEmpty() || isUnder(info.name(), filter))
                .sorted(Comparator.comparing(LoggerInfo::name))
                .toList();
    }

    @Override
    public synchronized LogLevelOverride setLevel(String logger, LogLevel level, Integer ttlMinutes, String changedBy) {
        String name = validLoggerName(logger);
        if (level == null) {
            throw new ObjectNotValidException("Parametr 'level' jest wymagany");
        }
        int ttl = ttlMinutes == null ? defaultTtlMinutes : ttlMinutes;
        if (ttl < 1 || ttl > maxTtlMinutes) {
            throw new ObjectNotValidException("Parametr 'ttlMinutes' musi byc z zakresu 1-" + maxTtlMinutes);
        }

        ActiveOverride existing = active.get(name);
        // przy ponownej zmianie wracamy do poziomu sprzed PIERWSZEJ zmiany, a nie do poprzedniego nadpisania
        LogLevel previous = existing != null ? existing.override().previousLevel() : control.getConfiguredLevel(name);
        if (existing != null) {
            cancel(existing);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LogLevelOverride override = new LogLevelOverride(name, level, previous, now, now.plusMinutes(ttl), changedBy);
        control.setLevel(name, level);
        ScheduledFuture<?> future = scheduler.schedule(() -> revert(override), ttl, TimeUnit.MINUTES);
        active.put(name, new ActiveOverride(override, future));

        log.info("Log level of '{}' set to {} (was {}) by {} for {} min, reverts at {}",
                name, level, describe(previous), changedBy, ttl, override.revertsAt());
        return override;
    }

    @Override
    public synchronized void resetLevel(String logger, String changedBy) {
        String name = validLoggerName(logger);
        ActiveOverride existing = active.remove(name);
        if (existing == null) {
            throw new ObjectNotFoundException("Brak aktywnego nadpisania poziomu dla loggera " + name);
        }
        cancel(existing);
        control.setLevel(name, existing.override().previousLevel());
        log.info("Log level of '{}' restored to {} by {}", name, describe(existing.override().previousLevel()), changedBy);
    }

    private synchronized void revert(LogLevelOverride expected) {
        ActiveOverride current = active.get(expected.logger());
        if (current == null || current.override() != expected) {
            return; // w miedzyczasie zastapione nowsza zmiana albo juz przywrocone
        }
        active.remove(expected.logger());
        control.setLevel(expected.logger(), expected.previousLevel());
        log.info("Log level of '{}' automatically restored to {} after TTL", expected.logger(), describe(expected.previousLevel()));
    }

    @PreDestroy
    synchronized void shutdown() {
        scheduler.shutdownNow();
        active.values().forEach(o -> control.setLevel(o.override().logger(), o.override().previousLevel()));
        active.clear();
    }

    private String validLoggerName(String logger) {
        String name = logger == null ? "" : logger.trim();
        if (!LOGGER_NAME.matcher(name).matches() || allowedPrefixes.stream().noneMatch(prefix -> isUnder(name, prefix))) {
            throw new ObjectNotValidException("Logger '" + name + "' nie jest dozwolony. Dozwolone prefiksy: " + allowedPrefixes);
        }
        return name;
    }

    private static boolean isUnder(String name, String prefix) {
        return name.equals(prefix) || name.startsWith(prefix + ".");
    }

    private static void cancel(ActiveOverride existing) {
        if (existing.future() != null) {
            existing.future().cancel(false);
        }
    }

    private static String describe(LogLevel level) {
        return level == null ? "inherited" : level.name();
    }

    private record ActiveOverride(LogLevelOverride override, ScheduledFuture<?> future) {
    }
}
