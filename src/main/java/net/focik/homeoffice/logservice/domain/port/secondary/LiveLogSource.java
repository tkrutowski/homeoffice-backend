package net.focik.homeoffice.logservice.domain.port.secondary;

import net.focik.homeoffice.logservice.domain.model.LiveLogsResult;
import net.focik.homeoffice.logservice.domain.model.LogLevel;

import java.util.Set;

/**
 * Zrodlo logow "na zywo" biezacej instancji (ostatnie wpisy trzymane w pamieci procesu).
 */
public interface LiveLogSource {
    /**
     * @param afterSeq numer ostatniego znanego klientowi wpisu; {@code null} = od poczatku, zwraca ostatnie
     *                 {@code limit} wpisow
     * @param levels   filtr poziomow, pusty = wszystkie
     * @param limit    maksymalna liczba zwracanych wpisow
     */
    LiveLogsResult entriesAfter(Long afterSeq, Set<LogLevel> levels, int limit);
}
