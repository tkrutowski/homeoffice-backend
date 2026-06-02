package net.focik.homeoffice.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudiobookAvailability {
    private Integer bookId;
    private String title;
    private String author;
    private List<AudiobookPlatformResult> results;
    private LocalDateTime checkedAt;
}
