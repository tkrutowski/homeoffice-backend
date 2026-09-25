package net.focik.homeoffice.logservice.infrastructure.live;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import net.focik.homeoffice.logservice.domain.model.LiveLogsResult;
import net.focik.homeoffice.logservice.domain.model.LogEntry;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LogbackLiveLogAdapterTest {

    private LoggerContext context;
    private Logger logger;
    private LogbackLiveLogAdapter adapter;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();
        logger = context.getLogger("net.focik.homeoffice.SomeService");
        adapter = newAdapter(5);
    }

    private LogbackLiveLogAdapter newAdapter(int capacity) {
        LogbackLiveLogAdapter created = new LogbackLiveLogAdapter(capacity, "ec2");
        created.setContext(context);
        created.start();
        return created;
    }

    private void log(Level level, String message) {
        adapter.doAppend(new LoggingEvent(Logger.class.getName(), logger, level, message, null, null));
    }

    private static List<String> messages(LiveLogsResult result) {
        return result.entries().stream().map(LogEntry::getMessage).toList();
    }

    @Test
    void firstRequestWithoutCursorReturnsTailAndLatestCursor() {
        for (int i = 1; i <= 5; i++) {
            log(Level.INFO, "m" + i);
        }

        LiveLogsResult result = adapter.entriesAfter(null, Set.of(), 3);

        assertEquals(List.of("m3", "m4", "m5"), messages(result));
        assertEquals(5, result.cursor());
        assertFalse(result.gap());
        assertFalse(result.hasMore());
        assertEquals("ec2", result.instance());
    }

    @Test
    void emptyBufferReturnsEmptyResultWithZeroCursor() {
        LiveLogsResult result = adapter.entriesAfter(null, Set.of(), 10);

        assertTrue(result.entries().isEmpty());
        assertEquals(0, result.cursor());
        assertFalse(result.gap());
    }

    @Test
    void pollingWithCursorReturnsOnlyNewEntries() {
        log(Level.INFO, "a");
        log(Level.INFO, "b");
        long cursor = adapter.entriesAfter(null, Set.of(), 10).cursor();

        assertTrue(adapter.entriesAfter(cursor, Set.of(), 10).entries().isEmpty());

        log(Level.INFO, "c");
        LiveLogsResult next = adapter.entriesAfter(cursor, Set.of(), 10);

        assertEquals(List.of("c"), messages(next));
        assertEquals(cursor + 1, next.cursor());
        assertFalse(next.gap());
    }

    @Test
    void levelFilterSkipsOtherLevelsButCursorAdvancesPastThem() {
        log(Level.INFO, "info");
        log(Level.ERROR, "error");
        log(Level.WARN, "warn");

        LiveLogsResult result = adapter.entriesAfter(0L, Set.of(LogLevel.ERROR), 10);

        assertEquals(List.of("error"), messages(result));
        assertEquals(3, result.cursor());

        log(Level.INFO, "another info");
        LiveLogsResult next = adapter.entriesAfter(result.cursor(), Set.of(LogLevel.ERROR), 10);
        assertTrue(next.entries().isEmpty());
        assertEquals(4, next.cursor());
    }

    @Test
    void limitSetsHasMoreAndCursorAtLastReturnedEntry() {
        for (int i = 1; i <= 4; i++) {
            log(Level.INFO, "m" + i);
        }

        LiveLogsResult first = adapter.entriesAfter(0L, Set.of(), 2);
        assertEquals(List.of("m1", "m2"), messages(first));
        assertTrue(first.hasMore());
        assertEquals(2, first.cursor());

        LiveLogsResult second = adapter.entriesAfter(first.cursor(), Set.of(), 2);
        assertEquals(List.of("m3", "m4"), messages(second));
        assertFalse(second.hasMore());
        assertEquals(4, second.cursor());
    }

    @Test
    void evictedEntriesAreReportedAsGap() {
        for (int i = 1; i <= 8; i++) {
            log(Level.INFO, "m" + i);
        }

        LiveLogsResult stale = adapter.entriesAfter(1L, Set.of(), 10);

        assertTrue(stale.gap());
        assertEquals(List.of("m4", "m5", "m6", "m7", "m8"), messages(stale));

        LiveLogsResult fresh = adapter.entriesAfter(3L, Set.of(), 10);
        assertFalse(fresh.gap());
        assertEquals(List.of("m4", "m5", "m6", "m7", "m8"), messages(fresh));
    }

    @Test
    void cursorFromBeforeRestartReturnsTailAndGap() {
        log(Level.INFO, "after restart");

        LiveLogsResult result = adapter.entriesAfter(500L, Set.of(), 10);

        assertTrue(result.gap());
        assertEquals(List.of("after restart"), messages(result));
        assertEquals(1, result.cursor());
    }

    @Test
    void entryHasFormattedFieldsInstanceAndThrowableAppendedToMessage() {
        adapter.doAppend(new LoggingEvent(Logger.class.getName(), logger, Level.ERROR, "boom {}",
                new IllegalStateException("bad state"), new Object[]{"x"}));

        LogEntry entry = adapter.entriesAfter(null, Set.of(), 10).entries().get(0);

        assertEquals("ERROR", entry.getLevel());
        assertEquals("ec2", entry.getInstance());
        assertNotNull(entry.getTimestamp());
        assertNotNull(entry.getThread());
        // nazwa krotsza niz 36 znakow zostaje bez zmian, tak jak we wzorcu pliku (%logger{36})
        assertEquals("net.focik.homeoffice.SomeService", entry.getLogger());
        assertTrue(entry.getMessage().startsWith("boom x\njava.lang.IllegalStateException: bad state\n"), entry.getMessage());
        assertFalse(entry.getMessage().contains("\r"));
        assertFalse(entry.getMessage().endsWith("\n"));
    }

    @Test
    void controllerOwnLogsAreExcluded() {
        Logger controllerLogger = context.getLogger("net.focik.homeoffice.logservice.api.LogsController");
        adapter.doAppend(new LoggingEvent(Logger.class.getName(), controllerLogger, Level.INFO, "poll", null, null));
        log(Level.INFO, "kept");

        assertEquals(List.of("kept"), messages(adapter.entriesAfter(null, Set.of(), 10)));
    }

    @Test
    void attachHooksIntoRootLoggerAndDetachRemovesIt() {
        assumeLogback();
        LogbackLiveLogAdapter attached = new LogbackLiveLogAdapter(10, "local");
        attached.attach();
        try {
            LoggerFactory.getLogger("live.logs.test").error("visible in live buffer");

            List<String> messages = attached.entriesAfter(null, Set.of(), 100).entries().stream()
                    .map(LogEntry::getMessage).toList();
            assertTrue(messages.contains("visible in live buffer"), messages.toString());
        } finally {
            attached.detach();
        }

        LoggerFactory.getLogger("live.logs.test").error("after detach");
        assertFalse(attached.entriesAfter(null, Set.of(), 100).entries().stream()
                .anyMatch(e -> e.getMessage().equals("after detach")));
    }

    private static void assumeLogback() {
        org.junit.jupiter.api.Assumptions.assumeTrue(LoggerFactory.getILoggerFactory() instanceof LoggerContext);
    }
}
