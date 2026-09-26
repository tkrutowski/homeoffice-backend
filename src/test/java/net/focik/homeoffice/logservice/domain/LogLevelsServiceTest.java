package net.focik.homeoffice.logservice.domain;

import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogLevelOverride;
import net.focik.homeoffice.logservice.domain.model.LogLevelsInfo;
import net.focik.homeoffice.logservice.domain.model.LoggerInfo;
import net.focik.homeoffice.logservice.domain.port.secondary.LogLevelControl;
import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;
import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LogLevelsServiceTest {

    private static final String PACKAGE = "net.focik.homeoffice.finance";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 10, 0);

    private LogLevelControl control;
    private ScheduledExecutorService scheduler;
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final List<ScheduledFuture<?>> futures = new ArrayList<>();
    private LogLevelsService service;

    @BeforeEach
    void setUp() {
        control = mock(LogLevelControl.class);
        when(control.getRootLevel()).thenReturn(LogLevel.INFO);
        scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(inv -> {
            scheduledTasks.add(inv.getArgument(0));
            ScheduledFuture<?> future = mock(ScheduledFuture.class);
            futures.add(future);
            return future;
        });
        service = new LogLevelsService(control,
                List.of("net.focik.homeoffice", "org.hibernate.SQL"), 15, 1440, "ec2", CLOCK, scheduler);
    }

    @Test
    @DisplayName("should list only loggers under allowed prefixes, sorted by name")
    void getLoggers_returnsOnlyAllowedSorted() {
        when(control.getLoggers()).thenReturn(List.of(
                new LoggerInfo("org.hibernate.SQL", null, LogLevel.INFO),
                new LoggerInfo("net.focik.homeoffice.goahead", LogLevel.DEBUG, LogLevel.DEBUG),
                new LoggerInfo("net.focik.homeofficeevil", null, LogLevel.INFO),
                new LoggerInfo("org.hibernate", null, LogLevel.INFO),
                new LoggerInfo("ROOT", LogLevel.INFO, LogLevel.INFO),
                new LoggerInfo("net.focik.homeoffice", null, LogLevel.INFO)));

        List<String> names = service.getLoggers(null).stream().map(LoggerInfo::name).toList();

        assertEquals(List.of("net.focik.homeoffice", "net.focik.homeoffice.goahead", "org.hibernate.SQL"), names);
    }

    @Test
    @DisplayName("should narrow the logger list to the given prefix")
    void getLoggers_filtersByPrefix() {
        when(control.getLoggers()).thenReturn(List.of(
                new LoggerInfo("net.focik.homeoffice.goahead.Foo", null, LogLevel.INFO),
                new LoggerInfo("net.focik.homeoffice.goaheadx.Bar", null, LogLevel.INFO),
                new LoggerInfo("net.focik.homeoffice.finance.Baz", null, LogLevel.INFO)));

        List<String> names = service.getLoggers(" net.focik.homeoffice.goahead ").stream().map(LoggerInfo::name).toList();

        assertEquals(List.of("net.focik.homeoffice.goahead.Foo"), names);
    }

    @Test
    @DisplayName("should apply the level, remember the previous one and schedule a revert with the default TTL")
    void setLevel_appliesLevelRemembersPreviousAndSchedulesRevertWithDefaultTtl() {
        when(control.getConfiguredLevel(PACKAGE)).thenReturn(LogLevel.WARN);

        LogLevelOverride override = service.setLevel(PACKAGE, LogLevel.DEBUG, null, "admin");

        verify(control).setLevel(PACKAGE, LogLevel.DEBUG);
        verify(scheduler).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.MINUTES));
        assertEquals(LogLevel.DEBUG, override.level());
        assertEquals(LogLevel.WARN, override.previousLevel());
        assertEquals(NOW, override.setAt());
        assertEquals(NOW.plusMinutes(15), override.revertsAt());
        assertEquals("admin", override.changedBy());
    }

    @Test
    @DisplayName("should restore the previous level and remove the override when the TTL expires")
    void ttlExpiry_restoresPreviousLevelAndRemovesOverride() {
        when(control.getConfiguredLevel(PACKAGE)).thenReturn(LogLevel.WARN);
        service.setLevel(PACKAGE, LogLevel.DEBUG, 30, "admin");

        scheduledTasks.getFirst().run();

        verify(control).setLevel(PACKAGE, LogLevel.WARN);
        assertTrue(service.getLevels().overrides().isEmpty());
    }

    @Test
    @DisplayName("should restore an inherited level as null when the TTL expires")
    void ttlExpiry_restoresInheritedLevelAsNull() {
        when(control.getConfiguredLevel(PACKAGE)).thenReturn(null);
        service.setLevel(PACKAGE, LogLevel.DEBUG, null, "admin");

        scheduledTasks.getFirst().run();

        verify(control).setLevel(PACKAGE, null);
    }

    @Test
    @DisplayName("should keep the original previous level and cancel the old timer when the level is set again")
    void setLevelAgain_keepsOriginalPreviousLevelCancelsOldTimerAndOldTimerIsNoOp() {
        when(control.getConfiguredLevel(PACKAGE)).thenReturn(LogLevel.WARN);
        service.setLevel(PACKAGE, LogLevel.DEBUG, 15, "admin");
        LogLevelOverride second = service.setLevel(PACKAGE, LogLevel.TRACE, 60, "admin");

        assertEquals(LogLevel.WARN, second.previousLevel());
        verify(futures.getFirst()).cancel(false);
        verify(control, times(1)).getConfiguredLevel(PACKAGE);

        // spozniony timer pierwszej zmiany nie moze przywrocic poziomu
        scheduledTasks.getFirst().run();
        verify(control, never()).setLevel(PACKAGE, LogLevel.WARN);
        assertEquals(1, service.getLevels().overrides().size());

        scheduledTasks.get(1).run();
        verify(control).setLevel(PACKAGE, LogLevel.WARN);
        assertTrue(service.getLevels().overrides().isEmpty());
    }

    @Test
    @DisplayName("should restore the level immediately and cancel the timer on reset")
    void resetLevel_restoresImmediatelyAndCancelsTimer() {
        when(control.getConfiguredLevel(PACKAGE)).thenReturn(LogLevel.INFO);
        service.setLevel(PACKAGE, LogLevel.DEBUG, 15, "admin");

        service.resetLevel(PACKAGE, "admin");

        verify(control).setLevel(PACKAGE, LogLevel.INFO);
        verify(futures.getFirst()).cancel(false);
        assertTrue(service.getLevels().overrides().isEmpty());
    }

    @Test
    @DisplayName("should throw not found when resetting a level without an active override")
    void resetLevel_withoutActiveOverrideThrowsNotFound() {
        assertThrows(ObjectNotFoundException.class, () -> service.resetLevel(PACKAGE, "admin"));
        verify(control, never()).setLevel(any(), any());
    }

    @Test
    @DisplayName("should accept only the allowed prefix itself and its subpackages")
    void setLevel_acceptsPrefixItselfAndSubpackagesOnly() {
        service.setLevel("net.focik.homeoffice", LogLevel.DEBUG, null, "admin");
        service.setLevel("org.hibernate.SQL", LogLevel.DEBUG, null, "admin");

        assertThrows(ObjectNotValidException.class, () -> service.setLevel("net.focik.homeofficeevil", LogLevel.DEBUG, null, "admin"));
        assertThrows(ObjectNotValidException.class, () -> service.setLevel("org.hibernate", LogLevel.DEBUG, null, "admin"));
        assertThrows(ObjectNotValidException.class, () -> service.setLevel("ROOT", LogLevel.DEBUG, null, "admin"));
        assertThrows(ObjectNotValidException.class, () -> service.setLevel("", LogLevel.DEBUG, null, "admin"));
        assertThrows(ObjectNotValidException.class, () -> service.setLevel(null, LogLevel.DEBUG, null, "admin"));
        assertThrows(ObjectNotValidException.class, () -> service.setLevel("net.focik.homeoffice.x y", LogLevel.DEBUG, null, "admin"));
    }

    @Test
    @DisplayName("should reject a missing level and a TTL out of range")
    void setLevel_rejectsMissingLevelAndTtlOutOfRange() {
        assertThrows(ObjectNotValidException.class, () -> service.setLevel(PACKAGE, null, null, "admin"));
        assertThrows(ObjectNotValidException.class, () -> service.setLevel(PACKAGE, LogLevel.DEBUG, 0, "admin"));
        assertThrows(ObjectNotValidException.class, () -> service.setLevel(PACKAGE, LogLevel.DEBUG, 1441, "admin"));
        verify(control, never()).setLevel(any(), any());

        service.setLevel(PACKAGE, LogLevel.DEBUG, 1440, "admin");
        verify(scheduler).schedule(any(Runnable.class), eq(1440L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("should return the config, the root level and the sorted overrides")
    void getLevels_returnsConfigRootLevelAndSortedOverrides() {
        service.setLevel("org.hibernate.SQL", LogLevel.DEBUG, null, "admin");
        service.setLevel(PACKAGE, LogLevel.TRACE, null, "admin");

        LogLevelsInfo info = service.getLevels();

        assertEquals("ec2", info.instance());
        assertEquals(LogLevel.INFO, info.rootLevel());
        assertEquals(List.of("net.focik.homeoffice", "org.hibernate.SQL"), info.allowedLoggers());
        assertEquals(15, info.defaultTtlMinutes());
        assertEquals(1440, info.maxTtlMinutes());
        assertEquals(List.of(PACKAGE, "org.hibernate.SQL"), info.overrides().stream().map(LogLevelOverride::logger).toList());
    }

    @Test
    @DisplayName("should restore all active overrides on shutdown")
    void shutdown_restoresAllActiveOverrides() {
        when(control.getConfiguredLevel(PACKAGE)).thenReturn(LogLevel.WARN);
        service.setLevel(PACKAGE, LogLevel.DEBUG, null, "admin");

        service.shutdown();

        ArgumentCaptor<LogLevel> restored = ArgumentCaptor.forClass(LogLevel.class);
        verify(control, times(2)).setLevel(eq(PACKAGE), restored.capture());
        assertEquals(LogLevel.WARN, restored.getAllValues().get(1));
        verify(scheduler).shutdownNow();
    }
}
