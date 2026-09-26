package net.focik.homeoffice.logservice.infrastructure;

import net.focik.homeoffice.logservice.domain.model.LogEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogParserTest {

    @Test
    @DisplayName("should parse a standard log line")
    void parseLog_parsesStandardLine() {
        LogEntry entry = LogParser.parseLog(
                "2026-08-14 10:19:34.704 [SpringApplicationShutdownHook] INFO  o.s.b.w.e.tomcat.GracefulShutdown - Commencing graceful shutdown. Waiting for active requests to complete");

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 8, 14, 10, 19, 34, 704_000_000), entry.getTimestamp());
        assertEquals("INFO", entry.getLevel());
        assertEquals("SpringApplicationShutdownHook", entry.getThread());
        assertEquals("o.s.b.w.e.tomcat.GracefulShutdown", entry.getLogger());
        assertEquals("Commencing graceful shutdown. Waiting for active requests to complete", entry.getMessage());
    }

    @Test
    @DisplayName("should handle WARN padding and brackets in the logger name")
    void parseLog_handlesWarnPaddingAndBracketsInLogger() {
        LogEntry warn = LogParser.parseLog(
                "2026-08-06 07:57:27.124 [main] WARN  org.hibernate.dialect.Dialect - HHH000511: The 5.7.38 version is no longer supported");
        LogEntry brackets = LogParser.parseLog(
                "2026-08-06 07:57:25.755 [main] INFO  o.a.c.c.C.[Tomcat].[localhost].[/] - Initializing Spring embedded WebApplicationContext");

        assertNotNull(warn);
        assertEquals("WARN", warn.getLevel());
        assertNotNull(brackets);
        assertEquals("main", brackets.getThread());
        assertEquals("o.a.c.c.C.[Tomcat].[localhost].[/]", brackets.getLogger());
        assertEquals("Initializing Spring embedded WebApplicationContext", brackets.getMessage());
    }

    @Test
    @DisplayName("should handle the ERROR level and a message containing the separator")
    void parseLog_handlesErrorLevelAndMessageContainingSeparator() {
        LogEntry entry = LogParser.parseLog(
                "2026-08-06 07:58:10.314 [http-nio-8077-exec-2] ERROR o.a.c.c.C.[.[.[.[dispatcherServlet] - Servlet.service() threw exception - details: x");

        assertNotNull(entry);
        assertEquals("ERROR", entry.getLevel());
        assertEquals("http-nio-8077-exec-2", entry.getThread());
        assertEquals("o.a.c.c.C.[.[.[.[dispatcherServlet]", entry.getLogger());
        assertEquals("Servlet.service() threw exception - details: x", entry.getMessage());
    }

    @Test
    @DisplayName("should return null for continuation lines")
    void parseLog_returnsNullForContinuationLines() {
        assertNull(LogParser.parseLog("\tat io.jsonwebtoken.impl.DefaultJwtParser.parse(DefaultJwtParser.java:682)"));
        assertNull(LogParser.parseLog("io.jsonwebtoken.ExpiredJwtException: JWT expired"));
        assertNull(LogParser.parseLog(""));
    }

    @Test
    @DisplayName("should append a stack trace to the previous entry")
    void parseLogs_appendsStackTraceToPreviousEntry() {
        List<LogEntry> entries = LogParser.parseLogs(List.of(
                "2026-08-06 07:58:10.184 [http-nio-8077-exec-1] INFO  o.s.web.servlet.DispatcherServlet - Completed initialization in 1 ms",
                "2026-08-06 07:58:10.314 [http-nio-8077-exec-2] ERROR o.a.c.c.C.[.[.[.[dispatcherServlet] - Servlet.service() threw exception",
                "io.jsonwebtoken.ExpiredJwtException: JWT expired",
                "\tat io.jsonwebtoken.impl.DefaultJwtParser.parse(DefaultJwtParser.java:682)",
                "2026-08-06 07:58:11.000 [main] INFO  n.f.homeoffice.Foo - next"));

        assertEquals(3, entries.size());
        assertEquals("Completed initialization in 1 ms", entries.get(0).getMessage());
        assertEquals("Servlet.service() threw exception\n"
                + "io.jsonwebtoken.ExpiredJwtException: JWT expired\n"
                + "\tat io.jsonwebtoken.impl.DefaultJwtParser.parse(DefaultJwtParser.java:682)",
                entries.get(1).getMessage());
        assertEquals("next", entries.get(2).getMessage());
    }

    @Test
    @DisplayName("should append a multiline message and skip leading garbage")
    void parseLogs_appendsMultilineMessageAndSkipsLeadingGarbage() {
        List<LogEntry> entries = LogParser.parseLogs(List.of(
                "\tat orphan.Line(Foo.java:1)",
                "2026-08-06 07:57:27.140 [main] INFO  o.hibernate.orm.connections.pooling - HHH10001005: Database info:",
                "\tDatabase driver: undefined/unknown",
                "\tDatabase version: 5.7.38"));

        assertEquals(1, entries.size());
        assertEquals("HHH10001005: Database info:\n\tDatabase driver: undefined/unknown\n\tDatabase version: 5.7.38",
                entries.get(0).getMessage());
    }

    @Test
    @DisplayName("should return an empty list when there are no lines")
    void parseLogs_returnsEmptyListForNoLines() {
        assertTrue(LogParser.parseLogs(List.of()).isEmpty());
    }
}
