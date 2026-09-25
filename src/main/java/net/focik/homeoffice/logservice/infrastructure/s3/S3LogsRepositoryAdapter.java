package net.focik.homeoffice.logservice.infrastructure.s3;

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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Czyta logi wysylane przez {@code S3LogAppender} jako obiekty
 * {@code logs/homeoffice-YYYY-MM-DD-<epochMillis>-<instance>.log} (starsze obiekty sa bez czlonu {@code -<instance>}
 * i dostaja nazwe instancji {@value #LEGACY_INSTANCE}).
 * <p>
 * Kazdy obiekt to jeden flush appendera jednej instancji: zawiera wpisy z okresu od poprzedniego flusha do momentu
 * {@code epochMillis} z klucza, wiec obiekty o epochu wczesniejszym niz {@code from} mozna pominac bez pobierania.
 * Filtr instancji dziala na kluczu, wiec obiekty innych instancji tez nie sa pobierane. Wpisy z ostatnich
 * kilku minut (do flusha) nie sa jeszcze w S3.
 */
@Slf4j
@Component
@Profile("!dev")
public class S3LogsRepositoryAdapter implements LogsRepository {

    static final String LEGACY_INSTANCE = "unknown";

    private static final String KEY_PREFIX = "logs/homeoffice-";
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^logs/homeoffice-\\d{4}-\\d{2}-\\d{2}-(\\d+)(?:-([A-Za-z0-9_.-]+))?\\.log$");
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    // flushIntervalSeconds z logback-spring.xml (300 s) + zapas na opoznienie uploadu
    private static final Duration FLUSH_MARGIN = Duration.ofMinutes(6);

    private final S3Client s3Client;
    private final String bucketName;

    public S3LogsRepositoryAdapter(S3Client s3Client, @Value("${aws.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public LogResult find(LogQuery query) {
        try {
            List<LogObject> objects = findObjects(query);
            log.debug("Reading {} log objects from S3 for {} - {}", objects.size(), query.from(), query.to());

            List<LogEntry> entries = new ArrayList<>();
            for (LogObject object : objects) {
                readObject(object, query, entries);
                if (entries.size() > query.limit()) {
                    break;
                }
            }
            entries.sort(Comparator.comparing(LogEntry::getTimestamp));
            return LogResult.limited(entries, query.limit());
        } catch (SdkException | IOException | UncheckedIOException e) {
            throw new LogsReadException("Nie udalo sie odczytac logow z S3 (bucket " + bucketName + "): " + e.getMessage(), e);
        }
    }

    private List<LogObject> findObjects(LogQuery query) {
        long fromEpoch = toEpochMillis(query.from());
        long toEpoch = toEpochMillis(query.to()) + FLUSH_MARGIN.toMillis();

        List<LogObject> matching = new ArrayList<>();
        // data w kluczu to dzien flusha, wiec dzien po 'to' moze zawierac pierwszy flush z wpisami sprzed 'to'
        for (LocalDate day = query.from().toLocalDate(); !day.isAfter(query.to().toLocalDate().plusDays(1)); day = day.plusDays(1)) {
            for (String key : listKeys(KEY_PREFIX + day + "-")) {
                Matcher matcher = KEY_PATTERN.matcher(key);
                if (!matcher.matches()) {
                    continue;
                }
                long epoch = Long.parseLong(matcher.group(1));
                String instance = matcher.group(2) != null ? matcher.group(2) : LEGACY_INSTANCE;
                boolean instanceMatches = query.instance() == null || query.instance().equalsIgnoreCase(instance);
                if (instanceMatches && epoch >= fromEpoch && epoch <= toEpoch) {
                    matching.add(new LogObject(epoch, key, instance));
                }
            }
        }
        return matching.stream()
                .sorted(Comparator.comparingLong(LogObject::epoch))
                .toList();
    }

    private List<String> listKeys(String prefix) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;
        do {
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build());
            response.contents().stream().map(S3Object::key).forEach(keys::add);
            continuationToken = Boolean.TRUE.equals(response.isTruncated()) ? response.nextContinuationToken() : null;
        } while (continuationToken != null);
        return keys;
    }

    private void readObject(LogObject object, LogQuery query, List<LogEntry> entries) throws IOException {
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(object.key()).build());
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (LogEntry entry : LogParser.parseLogs(br.lines()::iterator)) {
                if (query.matches(entry)) {
                    entry.setInstance(object.instance());
                    entries.add(entry);
                }
            }
        }
    }

    private static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    private record LogObject(long epoch, String key, String instance) {
    }
}
