package net.focik.homeoffice.logservice.domain.port.primary;

import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogResult;

import java.time.LocalDateTime;
import java.util.Set;

public interface GetLogsUseCase {
    /**
     * @param levels   filtr poziomow, {@code null} lub pusty = wszystkie
     * @param limit    maksymalna liczba wpisow, {@code null} = wartosc domyslna
     * @param instance nazwa instancji aplikacji (np. ec2, synology, local), {@code null} lub pusta = wszystkie
     */
    LogResult getLogs(LocalDateTime from, LocalDateTime to, Set<LogLevel> levels, Integer limit, String instance);

    /**
     * Logi od poczatku dzisiejszego dnia do teraz.
     */
    LogResult getTodayLogs(Set<LogLevel> levels, Integer limit, String instance);
}
