package net.focik.homeoffice.logservice.infrastructure;

import net.focik.homeoffice.logservice.domain.model.LogEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsuje logi zapisywane przez {@code logback-spring.xml} we wzorcu
 * {@code %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n}.
 * Linie niepasujace do wzorca (stacktrace, wieloliniowe komunikaty) sa doklejane do poprzedniego wpisu.
 */
public class LogParser {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Pattern logPattern = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(.*?)] (TRACE|DEBUG|INFO|WARN|ERROR)\\s+(\\S+) - (.*)$"
    );

    /**
     * Parsuje wszystkie linie, laczac linie kontynuacji z poprzednim wpisem.
     * Linie przed pierwszym poprawnym wpisem sa pomijane.
     */
    public static List<LogEntry> parseLogs(Iterable<String> logLines) {
        List<LogEntry> logEntries = new ArrayList<>();
        LogEntry current = null;
        StringBuilder message = null;

        for (String logLine : logLines) {
            LogEntry entry = parseLog(logLine);
            if (entry != null) {
                if (current != null) {
                    current.setMessage(message.toString());
                }
                current = entry;
                message = new StringBuilder(entry.getMessage());
                logEntries.add(entry);
            } else if (current != null) {
                message.append('\n').append(logLine);
            }
        }
        if (current != null) {
            current.setMessage(message.toString());
        }
        return logEntries;
    }

    /**
     * Parsuje pojedyncza linie poczatku wpisu; zwraca {@code null}, gdy linia nie pasuje do wzorca.
     */
    public static LogEntry parseLog(String logLine) {
        Matcher matcher = logPattern.matcher(logLine);
        if (!matcher.matches()) {
            return null;
        }

        LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), formatter);
        String thread = matcher.group(2);
        String level = matcher.group(3);
        String logger = matcher.group(4);
        String message = matcher.group(5);

        return new LogEntry(timestamp, level, thread, logger, message, null);
    }
}
