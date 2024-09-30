package net.focik.homeoffice.logservice.domain.port.secondary;

import net.focik.homeoffice.logservice.domain.model.LogEntry;

import java.time.LocalDateTime;
import java.util.List;

public interface LogsRepository {
    List<LogEntry> getLogsByDate(LocalDateTime from, LocalDateTime to);
    List<LogEntry> getTodayLogs();
}
