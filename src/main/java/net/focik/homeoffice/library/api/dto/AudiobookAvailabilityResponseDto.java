package net.focik.homeoffice.library.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AudiobookAvailabilityResponseDto {
    @JsonProperty("bookId")
    private Integer bookId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("author")
    private String author;

    @JsonProperty("results")
    private List<PlatformResultDto> results;

    @JsonProperty("checkedAt")
    private LocalDateTime checkedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformResultDto {
        @JsonProperty("bookstoreId")
        private Integer bookstoreId;

        @JsonProperty("platformName")
        private String platformName;

        @JsonProperty("available")
        private Boolean available;

        @JsonProperty("url")
        private String url;

        @JsonProperty("error")
        private String error;
    }
}
