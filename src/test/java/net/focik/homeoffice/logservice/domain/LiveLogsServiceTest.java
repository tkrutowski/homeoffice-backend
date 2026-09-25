package net.focik.homeoffice.logservice.domain;

import net.focik.homeoffice.logservice.domain.model.LiveLogsResult;
import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.port.secondary.LiveLogSource;
import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LiveLogsServiceTest {

    private LiveLogSource source;
    private LiveLogsService service;

    @BeforeEach
    void setUp() {
        source = mock(LiveLogSource.class);
        LiveLogsResult empty = new LiveLogsResult("test", List.of(), 0, false, false);
        when(source.entriesAfter(any(), any(), anyInt())).thenReturn(empty);
        service = new LiveLogsService(source);
    }

    private int capturedLimit() {
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(source).entriesAfter(any(), any(), limit.capture());
        return limit.getValue();
    }

    @Test
    void usesDefaultLimitAndAllLevelsWhenNotGiven() {
        service.getLiveLogs(null, null, null);

        verify(source).entriesAfter(null, Set.of(), LiveLogsService.DEFAULT_LIMIT);
    }

    @Test
    void capsLimitAndRaisesNonPositiveToOne() {
        service.getLiveLogs(5L, Set.of(LogLevel.ERROR), 1_000_000);
        assertEquals(LiveLogsService.MAX_LIMIT, capturedLimit());

        clearInvocations(source);
        service.getLiveLogs(5L, null, -3);
        assertEquals(1, capturedLimit());
    }

    @Test
    void passesCursorAndLevelsThrough() {
        service.getLiveLogs(42L, Set.of(LogLevel.WARN), 10);

        verify(source).entriesAfter(eq(42L), eq(Set.of(LogLevel.WARN)), eq(10));
    }

    @Test
    void rejectsNegativeCursor() {
        assertThrows(ObjectNotValidException.class, () -> service.getLiveLogs(-1L, null, null));
        verifyNoInteractions(source);
    }
}
