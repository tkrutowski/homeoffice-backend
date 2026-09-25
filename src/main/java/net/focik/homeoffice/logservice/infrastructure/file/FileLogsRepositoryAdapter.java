package net.focik.homeoffice.logservice.infrastructure.file;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.logservice.domain.exceptions.LogsReadException;
import net.focik.homeoffice.logservice.domain.model.LogEntry;
import net.focik.homeoffice.logservice.domain.model.LogQuery;
import net.focik.homeoffice.logservice.domain.model.LogResult;
import net.focik.homeoffice.logservice.domain.port.secondary.LogsRepository;
import net.focik.homeoffice.logservice.infrastructure.LogParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Czyta lokalne pliki logow (biezacy {@code homeoffice.log} i archiwa {@code homeoffice.log.YYYY-MM-DD.gz}).
 * Uzywany tylko na profilu {@code dev}, gdzie nie dziala {@code S3LogAppender}; lokalne pliki sa nietrwale
 * (brak wolumenu w kontenerze) i trzymane tylko 7 dni.
 */
@Slf4j
@Component
@Profile("dev")
public class FileLogsRepositoryAdapter implements LogsRepository {
    private static final Pattern ARCHIVE_PATTERN = Pattern.compile("^homeoffice\\.log\\.(\\d{4}-\\d{2}-\\d{2})\\.gz$");

    @Value("${logging.file.path}")
    private String logDirectory;

    @Override
    public LogResult find(LogQuery query) {
        List<LogEntry> entries = new ArrayList<>();

        try (Stream<Path> files = Files.list(Paths.get(logDirectory))) {
            files.filter(path -> shouldRead(path, query)).forEach(path -> readFile(path, query, entries));
        } catch (IOException e) {
            throw new LogsReadException("Nie udalo sie odczytac katalogu logow " + logDirectory + ": " + e.getMessage(), e);
        }

        entries.sort(Comparator.comparing(LogEntry::getTimestamp));
        return LogResult.limited(entries, query.limit());
    }

    private boolean shouldRead(Path path, LogQuery query) {
        String fileName = path.getFileName().toString();
        Matcher archive = ARCHIVE_PATTERN.matcher(fileName);
        if (archive.matches()) {
            LocalDate fileDate = LocalDate.parse(archive.group(1));
            return !fileDate.isBefore(query.from().toLocalDate()) && !fileDate.isAfter(query.to().toLocalDate());
        }
        return fileName.endsWith(".log");
    }

    private void readFile(Path path, LogQuery query, List<LogEntry> entries) {
        try (BufferedReader br = open(path)) {
            LogParser.parseLogs(br.lines()::iterator).stream()
                    .filter(query::matches)
                    .forEach(entries::add);
        } catch (IOException | UncheckedIOException e) {
            log.error("Nie udalo sie odczytac pliku logow {}: {}", path, e.getMessage(), e);
        }
    }

    private BufferedReader open(Path path) throws IOException {
        if (path.getFileName().toString().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path)), StandardCharsets.UTF_8));
        }
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }
}
