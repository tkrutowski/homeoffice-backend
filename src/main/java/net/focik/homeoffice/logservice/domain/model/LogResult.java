package net.focik.homeoffice.logservice.domain.model;

import java.util.List;

/**
 * Wynik zapytania o logi; {@code truncated} = true oznacza, ze wpisow bylo wiecej niz {@code limit}.
 */
public record LogResult(List<LogEntry> entries, boolean truncated) {

    /**
     * Przycina posortowana liste do {@code limit} wpisow i ustawia flage {@code truncated}.
     */
    public static LogResult limited(List<LogEntry> sortedEntries, int limit) {
        if (sortedEntries.size() <= limit) {
            return new LogResult(sortedEntries, false);
        }
        return new LogResult(List.copyOf(sortedEntries.subList(0, limit)), true);
    }
}
