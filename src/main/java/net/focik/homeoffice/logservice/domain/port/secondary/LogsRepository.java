package net.focik.homeoffice.logservice.domain.port.secondary;

import net.focik.homeoffice.logservice.domain.model.LogQuery;
import net.focik.homeoffice.logservice.domain.model.LogResult;

public interface LogsRepository {
    /**
     * Zwraca wpisy pasujace do zapytania, posortowane rosnaco po czasie i przyciete do {@code query.limit()}.
     */
    LogResult find(LogQuery query);
}
