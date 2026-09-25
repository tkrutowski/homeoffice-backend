package net.focik.homeoffice.logservice.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.logservice.domain.model.LiveLogsResult;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.port.primary.GetLiveLogsUseCase;
import net.focik.homeoffice.logservice.domain.port.secondary.LiveLogSource;
import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
class LiveLogsService implements GetLiveLogsUseCase {

    static final int DEFAULT_LIMIT = 200;
    static final int MAX_LIMIT = 2000;

    private final LiveLogSource liveLogSource;

    @Override
    public LiveLogsResult getLiveLogs(Long after, Set<LogLevel> levels, Integer limit) {
        if (after != null && after < 0) {
            throw new ObjectNotValidException("Parametr 'after' nie moze byc ujemny");
        }
        return liveLogSource.entriesAfter(after, levels == null ? Set.of() : levels, effectiveLimit(limit));
    }

    private int effectiveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
