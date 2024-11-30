package net.focik.homeoffice.logservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.logservice.domain.model.LogEntry;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.port.primary.GetLogsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/logs")
public class LogsController {
    final private GetLogsUseCase getLogsUseCase;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LOGS_READ_ALL','LOGS_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<LogEntry>> getTodayLogs(@RequestParam(value = "levels", required = false) Set<LogLevel> levels) {
        log.info("Request to get today's logs: levels = {}", levels);
        List<LogEntry> logs = getLogsUseCase.getLogs(levels);
        if (logs.isEmpty()) {
            log.info("No logs found.");
        } else {
            log.info("Found {} logs.", logs.size());
        }
        return new ResponseEntity<>(logs, HttpStatus.OK);
    }

    @GetMapping("/date")
    @PreAuthorize("hasAnyAuthority('LOGS_READ_ALL','LOGS_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<LogEntry>> getTodayLog(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "levels", required = false) Set<LogLevel> levels) {
        log.info("Request to get logs from {} to {}, {}", from, to, levels);
        List<LogEntry> logs = getLogsUseCase.getLogs(from, to, levels);
        if (logs.isEmpty()) {
            log.info("No logs found.");
        } else {
            log.info("Found {} logs.", logs.size());
        }
        return new ResponseEntity<>(logs, HttpStatus.OK);
    }
}
