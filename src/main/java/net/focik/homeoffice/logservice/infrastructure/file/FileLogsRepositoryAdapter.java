package net.focik.homeoffice.logservice.infrastructure.file;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.logservice.domain.model.LogEntry;
import net.focik.homeoffice.logservice.domain.port.secondary.LogsRepository;
import net.focik.homeoffice.logservice.infrastructure.LogParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
public class FileLogsRepositoryAdapter implements LogsRepository {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter fileNameDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${logging.file.name}")
    private String filePath;

    @Value("${logging.file.path}")
    private String logDirectory;

    @Override
    public List<LogEntry> getLogsByDate(LocalDateTime from, LocalDateTime to) {
        List<LogEntry> logEntries = new ArrayList<>();

        try {
            Files.list(Paths.get(logDirectory))
                    .filter(path -> path.toString().endsWith(".log") || path.toString().endsWith(".gz"))
                    .forEach(path -> {
                        try {
                            if (path.toString().endsWith(".gz") && path.toString().contains("homeoffice.log.")) {
                                String fileName = path.getFileName().toString();
                                // Wyodrębnij datę z nazwy pliku
                                String datePart = fileName.substring(15, 25); // Format "2024-09-19"
                                LocalDate fileDate = LocalDate.parse(datePart, fileNameDateFormatter);
                                if (!fileDate.isBefore(from.toLocalDate()) && !fileDate.isAfter(to.toLocalDate())) {
                                    readGzLogFile(path, logEntries, from, to);
                                }
                            } else {
                                readLogFile(path, logEntries, from, to);
                            }
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return logEntries;
    }

    @Override
    public List<LogEntry> getTodayLogs() {
        List<LogEntry> logEntries = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                LogEntry logEntry = LogParser.parseLog(line);
                if (logEntry != null) {
                    logEntries.add(logEntry);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return logEntries;
    }

    private void readLogFile(Path path, List<LogEntry> logEntries, LocalDateTime from, LocalDateTime to) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                LogEntry logEntry = LogParser.parseLog(line);
                if (logEntry != null && logEntry.getTimestamp().isAfter(from) && logEntry.getTimestamp().isBefore(to)) {
                    logEntries.add(logEntry);
                }
            }
        }
    }

    private void readGzLogFile(Path path, List<LogEntry> logEntries, LocalDateTime from, LocalDateTime to) throws IOException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(path.toFile()));
             BufferedReader br = new BufferedReader(new InputStreamReader(gzipInputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                LogEntry logEntry = LogParser.parseLog(line);
                if (logEntry != null && logEntry.getTimestamp().isAfter(from) && logEntry.getTimestamp().isBefore(to)) {
                    logEntries.add(logEntry);
                }
            }
        }
    }
}
