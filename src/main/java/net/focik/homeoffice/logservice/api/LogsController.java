package net.focik.homeoffice.logservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogResult;
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
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/logs")
public class LogsController {
    final private GetLogsUseCase getLogsUseCase;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LOGS_READ_ALL','LOGS_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<LogResult> getTodayLogs(
            @RequestParam(value = "levels", required = false) Set<LogLevel> levels,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "instance", required = false) String instance) {
        log.info("Request to get today's logs: levels = {}, limit = {}, instance = {}", levels, limit, instance);
        LogResult result = getLogsUseCase.getTodayLogs(levels, limit, instance);
        log.info("Found {} logs (truncated = {}).", result.entries().size(), result.truncated());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/date")
    @PreAuthorize("hasAnyAuthority('LOGS_READ_ALL','LOGS_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<LogResult> getLogsByDate(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "levels", required = false) Set<LogLevel> levels,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "instance", required = false) String instance) {
        log.info("Request to get logs from {} to {}, levels = {}, limit = {}, instance = {}", from, to, levels, limit, instance);
        LogResult result = getLogsUseCase.getLogs(from, to, levels, limit, instance);
        log.info("Found {} logs (truncated = {}).", result.entries().size(), result.truncated());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
