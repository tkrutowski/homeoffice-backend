package net.focik.homeoffice.logservice.infrastructure;

import net.focik.homeoffice.logservice.domain.model.LogEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    // Wyrażenie regularne do parsowania logu
    private static final Pattern logPattern = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(INFO|DEBUG|ERROR|WARN)\\s+(\\d+)\\s+---\\s+\\[([^\\]]+)]\\s+\\[([^\\]]+)]\\s+([^:]+)\\s*:\\s*(.*)"
    );
    public static List<LogEntry> parseLogs(List<String> logLines) {
        List<LogEntry> logEntries = new ArrayList<>();

        for (String logLine : logLines) {
            logEntries.add(parseLog(logLine));
        }

        return logEntries;
    }

    public static LogEntry parseLog(String logLine) {

        Matcher matcher = logPattern.matcher(logLine);

        if (!matcher.matches()) {
            return null; // Błędny format logu
        }

        LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1) + " " + matcher.group(2), formatter);
        String level = matcher.group(3);
        int processId = Integer.parseInt(matcher.group(4));
        String thread = matcher.group(5);
        String logger = matcher.group(7); // Nazwa klasy lub loggera
        String message = matcher.group(8);

        return new LogEntry(timestamp, level, processId, thread, logger, message);
    }
}
