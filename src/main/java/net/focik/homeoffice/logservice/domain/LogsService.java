package net.focik.homeoffice.logservice.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogQuery;
import net.focik.homeoffice.logservice.domain.model.LogResult;
import net.focik.homeoffice.logservice.domain.port.primary.GetLogsUseCase;
import net.focik.homeoffice.logservice.domain.port.secondary.LogsRepository;
import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
class LogsService implements GetLogsUseCase {

    static final int DEFAULT_LIMIT = 1000;
    static final int MAX_LIMIT = 5000;
    static final Duration MAX_RANGE = Duration.ofDays(7);

    private final LogsRepository logsRepository;

    @Override
    public LogResult getLogs(LocalDateTime from, LocalDateTime to, Set<LogLevel> levels, Integer limit, String instance) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new ObjectNotValidException("Parametr 'from' musi byc wczesniejszy niz 'to'");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new ObjectNotValidException("Maksymalny zakres dat to " + MAX_RANGE.toDays() + " dni");
        }
        return logsRepository.find(new LogQuery(from, to, levels == null ? Set.of() : levels, effectiveLimit(limit),
                instance == null || instance.isBlank() ? null : instance.trim()));
    }

    @Override
    public LogResult getTodayLogs(Set<LogLevel> levels, Integer limit, String instance) {
        LocalDateTime now = LocalDateTime.now();
        return getLogs(now.toLocalDate().atStartOfDay(), now.plusSeconds(1), levels, limit, instance);
    }

    private int effectiveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
