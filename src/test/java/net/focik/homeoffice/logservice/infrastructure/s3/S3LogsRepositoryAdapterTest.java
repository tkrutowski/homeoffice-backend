package net.focik.homeoffice.logservice.infrastructure.s3;

import net.focik.homeoffice.logservice.domain.exceptions.LogsReadException;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogQuery;
import net.focik.homeoffice.logservice.domain.model.LogResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3LogsRepositoryAdapterTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final String BUCKET = "test-bucket";

    private S3Client s3Client;
    private S3LogsRepositoryAdapter adapter;
    /** klucz S3 -> zawartosc obiektu */
    private final Map<String, String> objects = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        adapter = new S3LogsRepositoryAdapter(s3Client, BUCKET);

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenAnswer(inv -> {
            ListObjectsV2Request request = inv.getArgument(0);
            return ListObjectsV2Response.builder()
                    .contents(objects.keySet().stream()
                            .filter(key -> key.startsWith(request.prefix()))
                            .map(key -> S3Object.builder().key(key).build())
                            .toList())
                    .isTruncated(false)
                    .build();
        });
        when(s3Client.getObject(any(GetObjectRequest.class))).thenAnswer(inv -> {
            GetObjectRequest request = inv.getArgument(0);
            byte[] bytes = objects.get(request.key()).getBytes(StandardCharsets.UTF_8);
            return new ResponseInputStream<>(GetObjectResponse.builder().build(),
                    AbortableInputStream.create(new ByteArrayInputStream(bytes)));
        });
    }

    private static long epoch(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    private void putObject(LocalDateTime flushTime, String content) {
        objects.put("logs/homeoffice-" + flushTime.toLocalDate() + "-" + epoch(flushTime) + ".log", content);
    }

    private void putObject(LocalDateTime flushTime, String instance, String content) {
        objects.put("logs/homeoffice-" + flushTime.toLocalDate() + "-" + epoch(flushTime) + "-" + instance + ".log", content);
    }

    private static String line(String time, String level, String message) {
        return "2026-09-20 " + time + " [main] " + level + " n.f.h.Foo - " + message + "\n";
    }

    private static LogQuery query(LocalDateTime from, LocalDateTime to, Set<LogLevel> levels, int limit) {
        return new LogQuery(from, to, levels, limit, null);
    }

    @Test
    @DisplayName("should read matching objects, filter by time and level and sort the entries")
    void find_readsMatchingObjectsFiltersByTimeAndLevelAndSorts() {
        putObject(LocalDateTime.of(2026, 9, 20, 10, 5), line("10:00:00.000", "INFO ", "a") + line("10:04:00.000", "ERROR", "b")
                + "java.lang.IllegalStateException: boom\n\tat x.Y(Y.java:1)\n");
        putObject(LocalDateTime.of(2026, 9, 20, 10, 10), line("10:06:00.000", "ERROR", "c") + line("10:09:00.000", "WARN ", "d"));
        putObject(LocalDateTime.of(2026, 9, 20, 10, 15), line("10:11:00.000", "ERROR", "out of range"));

        LogResult result = adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 10, 3), LocalDateTime.of(2026, 9, 20, 10, 10),
                Set.of(LogLevel.ERROR), 100));

        assertFalse(result.truncated());
        assertEquals(2, result.entries().size());
        assertEquals("b\njava.lang.IllegalStateException: boom\n\tat x.Y(Y.java:1)", result.entries().get(0).getMessage());
        assertEquals("c", result.entries().get(1).getMessage());
    }

    @Test
    @DisplayName("should not download objects flushed before the from time")
    void find_doesNotDownloadObjectsFlushedBeforeFrom() {
        putObject(LocalDateTime.of(2026, 9, 20, 9, 0), line("08:58:00.000", "INFO ", "too early"));
        putObject(LocalDateTime.of(2026, 9, 20, 10, 5), line("10:00:00.000", "INFO ", "in range"));

        LogResult result = adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 10, 30), Set.of(), 100));

        assertEquals(1, result.entries().size());
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("should include the first object flushed after the to time because it holds entries before it")
    void find_includesFirstObjectFlushedAfterToBecauseItHoldsEntriesBeforeTo() {
        putObject(LocalDateTime.of(2026, 9, 20, 10, 20), line("10:14:00.000", "INFO ", "before to")
                + line("10:19:00.000", "INFO ", "after to"));

        LogResult result = adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 10, 16), Set.of(), 100));

        assertEquals(1, result.entries().size());
        assertEquals("before to", result.entries().get(0).getMessage());
    }

    @Test
    @DisplayName("should look into the next day for objects flushed after midnight")
    void find_looksIntoNextDayForObjectsFlushedAfterMidnight() {
        LocalDateTime flush = LocalDateTime.of(2026, 9, 21, 0, 3);
        objects.put("logs/homeoffice-2026-09-21-" + epoch(flush) + ".log", line("23:58:00.000", "INFO ", "late entry"));

        LogResult result = adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 23, 0), LocalDateTime.of(2026, 9, 21, 0, 0), Set.of(), 100));

        assertEquals(1, result.entries().size());
        assertEquals("late entry", result.entries().get(0).getMessage());
    }

    @Test
    @DisplayName("should truncate to the limit and keep the earliest entries")
    void find_truncatesToLimitAndKeepsEarliestEntries() {
        putObject(LocalDateTime.of(2026, 9, 20, 10, 5),
                line("10:01:00.000", "INFO ", "1") + line("10:02:00.000", "INFO ", "2") + line("10:03:00.000", "INFO ", "3"));

        LogResult result = adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 11, 0), Set.of(), 2));

        assertTrue(result.truncated());
        assertEquals(2, result.entries().size());
        assertEquals("1", result.entries().get(0).getMessage());
        assertEquals("2", result.entries().get(1).getMessage());
    }

    @Test
    @DisplayName("should ignore unrelated keys")
    void find_ignoresUnrelatedKeys() {
        objects.put("logs/homeoffice-2026-09-20-notanumber.log", "garbage");
        objects.put("logs/homeoffice-2026-09-20-" + epoch(LocalDateTime.of(2026, 9, 20, 10, 5)) + ".txt", "garbage");

        LogResult result = adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 11, 0), Set.of(), 100));

        assertTrue(result.entries().isEmpty());
        verify(s3Client, never()).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("should set the instance from the key and unknown for legacy keys")
    void find_setsInstanceFromKeyAndUnknownForLegacyKeys() {
        putObject(LocalDateTime.of(2026, 9, 20, 10, 5), "ec2", line("10:01:00.000", "INFO ", "from ec2"));
        putObject(LocalDateTime.of(2026, 9, 20, 10, 10), "home-office.1", line("10:06:00.000", "INFO ", "from home"));
        putObject(LocalDateTime.of(2026, 9, 20, 10, 15), line("10:11:00.000", "INFO ", "legacy"));

        LogResult result = adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 11, 0), Set.of(), 100));

        assertEquals(3, result.entries().size());
        assertEquals("ec2", result.entries().get(0).getInstance());
        assertEquals("home-office.1", result.entries().get(1).getInstance());
        assertEquals("unknown", result.entries().get(2).getInstance());
    }

    @Test
    @DisplayName("should filter by instance without downloading other instances' objects")
    void find_filtersByInstanceWithoutDownloadingOtherInstances() {
        putObject(LocalDateTime.of(2026, 9, 20, 10, 5), "ec2", line("10:01:00.000", "INFO ", "from ec2"));
        putObject(LocalDateTime.of(2026, 9, 20, 10, 10), "local", line("10:06:00.000", "INFO ", "from local"));
        putObject(LocalDateTime.of(2026, 9, 20, 10, 15), line("10:11:00.000", "INFO ", "legacy"));

        LogResult result = adapter.find(new LogQuery(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 11, 0), Set.of(), 100, "LOCAL"));

        assertEquals(1, result.entries().size());
        assertEquals("from local", result.entries().get(0).getMessage());
        assertEquals("local", result.entries().get(0).getInstance());
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));

        clearInvocations(s3Client);
        LogResult legacy = adapter.find(new LogQuery(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 11, 0), Set.of(), 100, "unknown"));
        assertEquals(1, legacy.entries().size());
        assertEquals("legacy", legacy.entries().get(0).getMessage());
    }

    @Test
    @DisplayName("should wrap an S3 failure in LogsReadException")
    void find_wrapsS3FailureInLogsReadException() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("Access Denied").statusCode(403).build());

        LogsReadException exception = assertThrows(LogsReadException.class, () -> adapter.find(query(
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 11, 0), Set.of(), 100)));

        assertTrue(exception.getMessage().contains(BUCKET));
    }
}
