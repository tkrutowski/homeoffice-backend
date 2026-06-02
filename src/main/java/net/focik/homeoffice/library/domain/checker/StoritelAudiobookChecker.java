package net.focik.homeoffice.library.domain.checker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.library.domain.model.AudiobookPlatformResult;
import net.focik.homeoffice.library.infrastructure.dto.BookstoreDbDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

@Log4j2
@Component
@RequiredArgsConstructor
public class StoritelAudiobookChecker implements AudiobookChecker {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String bookstoreUrl) {
        return bookstoreUrl != null && bookstoreUrl.contains("storytel.com");
    }

    @Override
    public AudiobookPlatformResult check(String title, String author, BookstoreDbDto bookstore) {
        try {
            String query = title + " " + author;
            String apiUrl = UriComponentsBuilder.fromUriString("https://api.storytel.net/search/client/web")
                    .queryParam("query", query)
                    .queryParam("store", "STHP-PL")
                    .queryParam("searchFor", "books")
                    .queryParam("includeFormats", "abook")
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(apiUrl, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode items = jsonResponse.get("items");

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String itemTitle = item.get("title").asText("");
                    if (titleMatches(itemTitle, title)) {
                        if (hasAudiobookFormat(item)) {
                            String shareUrl = item.get("shareUrl").asText();
                            return AudiobookPlatformResult.available(bookstore.getId(), "Storytel", shareUrl);
                        }
                    }
                }
            }

            return AudiobookPlatformResult.unavailable(bookstore.getId(), "Storytel");
        } catch (Exception e) {
            log.warn("Storytel audiobook check failed: {}", e.getMessage());
            return AudiobookPlatformResult.error(bookstore.getId(), "Storytel", e.getMessage());
        }
    }

    private boolean hasAudiobookFormat(JsonNode item) {
        JsonNode formats = item.get("formats");
        if (formats != null && formats.isArray()) {
            for (JsonNode format : formats) {
                String type = format.get("type").asText("");
                boolean isReleased = format.get("isReleased").asBoolean(false);
                if ("abook".equals(type) && isReleased) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean titleMatches(String found, String expected) {
        return found.toLowerCase().contains(expected.toLowerCase());
    }
}
