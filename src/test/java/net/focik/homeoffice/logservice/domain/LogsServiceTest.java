package net.focik.homeoffice.logservice.domain;

import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogQuery;
import net.focik.homeoffice.logservice.domain.model.LogResult;
import net.focik.homeoffice.logservice.domain.port.secondary.LogsRepository;
import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogsServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 9, 20, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 21, 0, 0);

    private LogsRepository repository;
    private LogsService service;

    @BeforeEach
    void setUp() {
        repository = mock(LogsRepository.class);
        when(repository.find(any())).thenReturn(new LogResult(List.of(), false));
        service = new LogsService(repository);
    }

    private LogQuery capturedQuery() {
        ArgumentCaptor<LogQuery> captor = ArgumentCaptor.forClass(LogQuery.class);
        verify(repository).find(captor.capture());
        return captor.getValue();
    }

    @Test
    void getLogs_usesDefaultLimitAndAllLevelsWhenNotGiven() {
        service.getLogs(FROM, TO, null, null, null);

        LogQuery query = capturedQuery();
        assertEquals(FROM, query.from());
        assertEquals(TO, query.to());
        assertEquals(Set.of(), query.levels());
        assertEquals(LogsService.DEFAULT_LIMIT, query.limit());
    }

    @Test
    void getLogs_capsLimitAndPassesLevels() {
        service.getLogs(FROM, TO, Set.of(LogLevel.ERROR), 1_000_000, null);

        LogQuery query = capturedQuery();
        assertEquals(LogsService.MAX_LIMIT, query.limit());
        assertEquals(Set.of(LogLevel.ERROR), query.levels());
    }

    @Test
    void getLogs_raisesNonPositiveLimitToOne() {
        service.getLogs(FROM, TO, null, -5, null);

        assertEquals(1, capturedQuery().limit());
    }

    @Test
    void getLogs_rejectsFromNotBeforeTo() {
        assertThrows(ObjectNotValidException.class, () -> service.getLogs(TO, FROM, null, null, null));
        assertThrows(ObjectNotValidException.class, () -> service.getLogs(FROM, FROM, null, null, null));
        assertThrows(ObjectNotValidException.class, () -> service.getLogs(null, TO, null, null, null));
        verifyNoInteractions(repository);
    }

    @Test
    void getLogs_rejectsRangeLongerThanMax() {
        assertThrows(ObjectNotValidException.class,
                () -> service.getLogs(FROM, FROM.plusDays(7).plusSeconds(1), null, null, null));
        verifyNoInteractions(repository);
    }

    @Test
    void getLogs_acceptsExactlyMaxRange() {
        service.getLogs(FROM, FROM.plusDays(7), null, null, null);

        verify(repository).find(any());
    }

    @Test
    void getLogs_trimsInstanceAndTreatsBlankAsAll() {
        service.getLogs(FROM, TO, null, null, "  synology ");
        assertEquals("synology", capturedQuery().instance());

        clearInvocations(repository);
        service.getLogs(FROM, TO, null, null, "   ");
        assertNull(capturedQuery().instance());
    }

    @Test
    void getTodayLogs_queriesFromStartOfToday() {
        service.getTodayLogs(null, null, null);

        LogQuery query = capturedQuery();
        assertEquals(LocalDate.now().atStartOfDay(), query.from());
        assertTrue(query.to().isAfter(query.from()));
    }
}
