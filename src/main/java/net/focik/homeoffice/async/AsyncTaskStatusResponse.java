package net.focik.homeoffice.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskStatusResponse {
    private AsyncTaskStatus status;
    private LocalDateTime updatedAt;
}
