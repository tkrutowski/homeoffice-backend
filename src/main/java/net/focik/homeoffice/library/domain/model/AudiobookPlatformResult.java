package net.focik.homeoffice.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudiobookPlatformResult {
    private Integer bookstoreId;
    private String platformName;
    private Boolean available;
    private String url;
    private String error;

    public static AudiobookPlatformResult available(Integer bookstoreId, String platformName, String url) {
        return AudiobookPlatformResult.builder()
                .bookstoreId(bookstoreId)
                .platformName(platformName)
                .available(true)
                .url(url)
                .error(null)
                .build();
    }

    public static AudiobookPlatformResult unavailable(Integer bookstoreId, String platformName) {
        return AudiobookPlatformResult.builder()
                .bookstoreId(bookstoreId)
                .platformName(platformName)
                .available(false)
                .url(null)
                .error(null)
                .build();
    }

    public static AudiobookPlatformResult timeout(Integer bookstoreId, String platformName) {
        return AudiobookPlatformResult.builder()
                .bookstoreId(bookstoreId)
                .platformName(platformName)
                .available(null)
                .url(null)
                .error("Timeout")
                .build();
    }

    public static AudiobookPlatformResult error(Integer bookstoreId, String platformName, String errorMessage) {
        return AudiobookPlatformResult.builder()
                .bookstoreId(bookstoreId)
                .platformName(platformName)
                .available(null)
                .url(null)
                .error(errorMessage)
                .build();
    }
}
