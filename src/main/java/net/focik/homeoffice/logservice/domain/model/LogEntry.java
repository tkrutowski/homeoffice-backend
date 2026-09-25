package net.focik.homeoffice.logservice.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEntry {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    private String level;
    private String thread;
    private String logger;
    private String message;
    /** Instancja aplikacji, z ktorej pochodzi wpis (np. ec2, synology, local); null gdy nieznana (odczyt z pliku). */
    private String instance;
}
