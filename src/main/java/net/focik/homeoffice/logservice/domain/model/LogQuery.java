package net.focik.homeoffice.logservice.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Zapytanie o logi: przedzial czasu {@code [from, to)}, opcjonalny filtr poziomow (pusty = wszystkie)
 * maksymalna liczba zwracanych wpisow i opcjonalna nazwa instancji ({@code null} = wszystkie instancje).
 */
public record LogQuery(LocalDateTime from, LocalDateTime to, Set<LogLevel> levels, int limit, String instance) {

    public boolean matches(LogEntry entry) {
        LocalDateTime timestamp = entry.getTimestamp();
        if (timestamp.isBefore(from) || !timestamp.isBefore(to)) {
            return false;
        }
        return levels.isEmpty() || levels.contains(LogLevel.valueOf(entry.getLevel()));
    }
}
