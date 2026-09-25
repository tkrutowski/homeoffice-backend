package net.focik.homeoffice.logservice.domain.port.primary;

import net.focik.homeoffice.logservice.domain.model.LiveLogsResult;
import net.focik.homeoffice.logservice.domain.model.LogLevel;

import java.util.Set;

public interface GetLiveLogsUseCase {
    /**
     * Logi "na zywo" biezacej instancji (polling z kursorem).
     *
     * @param after  kursor z poprzedniej odpowiedzi; {@code null} = pierwsze zapytanie (ostatnie wpisy)
     * @param levels filtr poziomow, {@code null} lub pusty = wszystkie
     * @param limit  maksymalna liczba wpisow, {@code null} = wartosc domyslna
     */
    LiveLogsResult getLiveLogs(Long after, Set<LogLevel> levels, Integer limit);
}
