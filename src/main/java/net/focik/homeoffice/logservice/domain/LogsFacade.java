package net.focik.homeoffice.logservice.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.logservice.domain.model.LogEntry;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.port.primary.GetLogsUseCase;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LogsFacade implements GetLogsUseCase {

    private final LogsService logsServicee;

    @Override
    public List<LogEntry> getLogs(LocalDateTime from, LocalDateTime to, Set<LogLevel> levels) {
        return logsServicee.getLogs(from, to, levels);
    }

    @Override
    public List<LogEntry> getLogs(Set<LogLevel> levels) {
        return logsServicee.getLogs(levels);
    }
}
