package net.focik.homeoffice.fileService.domain.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileInfo {
    private Integer id;
    private String name;
    private String url;
    private String type;
    private Integer size;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS", timezone = "Europe/Warsaw")
    private LocalDateTime uploadDate;
    private String description;
    @Transient
    private Resource content;
}
