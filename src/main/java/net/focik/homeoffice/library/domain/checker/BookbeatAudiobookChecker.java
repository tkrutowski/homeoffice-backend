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
public class BookbeatAudiobookChecker implements AudiobookChecker {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String bookstoreUrl) {
        return bookstoreUrl != null && bookstoreUrl.contains("bookbeat.com");
    }

    @Override
    public AudiobookPlatformResult check(String title, String author, BookstoreDbDto bookstore) {
        try {
            String query = title + " " + author;
            String apiUrl = UriComponentsBuilder.fromUriString("https://search-api.bookbeat.com/api/appsearch/books")
                    .queryParam("query", query)
                    .queryParam("market", "48")
                    .queryParam("includeErotic", "false")
                    .queryParam("limit", "50")
                    .queryParam("offset", "0")
                    .queryParam("v", "18")
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(apiUrl, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode books = jsonResponse.path("_embedded").get("books");

            if (books != null && books.isArray()) {
                for (JsonNode book : books) {
                    String bookTitle = book.get("title").asText("");
                    if (titleMatches(bookTitle, title)) {
                        if (hasAudiobook(book)) {
                            String shareUrl = book.get("shareurl").asText();
                            return AudiobookPlatformResult.available(bookstore.getId(), "Bookbeat", shareUrl);
                        }
                    }
                }
            }

            return AudiobookPlatformResult.unavailable(bookstore.getId(), "Bookbeat");
        } catch (Exception e) {
            log.warn("Bookbeat audiobook check failed: {}", e.getMessage());
            return AudiobookPlatformResult.error(bookstore.getId(), "Bookbeat", e.getMessage());
        }
    }

    private boolean hasAudiobook(JsonNode book) {
        JsonNode audiobookIsbn = book.get("audiobookisbn");
        return audiobookIsbn != null && !audiobookIsbn.isNull() && !audiobookIsbn.asText("").isEmpty();
    }

    private boolean titleMatches(String found, String expected) {
        return found.toLowerCase().contains(expected.toLowerCase());
    }
}
