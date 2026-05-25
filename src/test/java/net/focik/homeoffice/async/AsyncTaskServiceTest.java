package net.focik.homeoffice.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncTaskServiceTest {

    @Mock
    private AsyncTaskRepository asyncTaskRepository;

    private AsyncTaskService asyncTaskService;

    @BeforeEach
    void setUp() {
        asyncTaskService = new AsyncTaskService(asyncTaskRepository);
    }

    @Test
    void getLatestTaskStatus_ShouldReturnStatusAndUpdatedAt_WhenTaskExists() {
        String jobType = "ZUS_DRA";
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 24, 10, 30);
        AsyncTask task = AsyncTask.builder()
                .jobId("test-id")
                .jobType(jobType)
                .status(AsyncTaskStatus.SUCCEEDED)
                .updatedAt(updatedAt)
                .build();

        when(asyncTaskRepository.findFirstByJobTypeOrderByUpdatedAtDesc(jobType)).thenReturn(task);

        AsyncTaskStatusResponse response = asyncTaskService.getLatestTaskStatus(jobType);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AsyncTaskStatus.SUCCEEDED);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void getLatestTaskStatus_ShouldReturnNull_WhenTaskDoesNotExist() {
        String jobType = "NONEXISTENT";

        when(asyncTaskRepository.findFirstByJobTypeOrderByUpdatedAtDesc(jobType)).thenReturn(null);

        AsyncTaskStatusResponse response = asyncTaskService.getLatestTaskStatus(jobType);

        assertThat(response).isNull();
    }
}
