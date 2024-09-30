package net.focik.homeoffice.logservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEntry {
    private LocalDateTime timestamp;
    private String level;
    private int processId;
    private String thread;
    private String logger;
    private String message;
}
