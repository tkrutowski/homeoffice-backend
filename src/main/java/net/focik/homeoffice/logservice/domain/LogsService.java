package net.focik.homeoffice.logservice.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.logservice.domain.model.LogEntry;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.port.secondary.LogsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
class LogsService {

    private final LogsRepository logsRepository;

    public List<LogEntry> getLogs(LocalDateTime from, LocalDateTime to, Set<LogLevel> levels) {
        List<LogEntry> logsByDate = logsRepository.getLogsByDate(from, to);
        if (levels != null && !levels.isEmpty()) {
            logsByDate = logsByDate.stream()
                    .filter(logEntry -> levels.contains(LogLevel.valueOf(logEntry.getLevel().toUpperCase())))
                    .toList();
        }
        return logsByDate;
    }

    public List<LogEntry> getLogs(Set<LogLevel> levels) {
        List<LogEntry> todayLogs = logsRepository.getTodayLogs();
        if (levels != null && !levels.isEmpty()) {
            todayLogs = todayLogs.stream()
                    .filter(logEntry -> levels.contains(LogLevel.valueOf(logEntry.getLevel().toUpperCase())))
                    .toList();
        }
        return todayLogs;
    }
}
