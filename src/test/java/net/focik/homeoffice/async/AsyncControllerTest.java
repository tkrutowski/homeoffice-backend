package net.focik.homeoffice.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncControllerTest {

    @Mock
    private AsyncTaskService asyncTaskService;

    @InjectMocks
    private AsyncController asyncController;

    @Test
    void getLatestTaskStatus_ShouldReturnOkWithResponse_WhenTaskExists() {
        String jobType = "ZUS_DRA";
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 24, 10, 30);
        AsyncTaskStatusResponse response = new AsyncTaskStatusResponse(AsyncTaskStatus.RUNNING, updatedAt);

        when(asyncTaskService.getLatestTaskStatus(jobType)).thenReturn(response);

        ResponseEntity<AsyncTaskStatusResponse> result = asyncController.getLatestTaskStatus(jobType);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(AsyncTaskStatus.RUNNING);
        assertThat(result.getBody().getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void getLatestTaskStatus_ShouldReturnNotFound_WhenTaskDoesNotExist() {
        String jobType = "NONEXISTENT";

        when(asyncTaskService.getLatestTaskStatus(jobType)).thenReturn(null);

        ResponseEntity<AsyncTaskStatusResponse> result = asyncController.getLatestTaskStatus(jobType);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
