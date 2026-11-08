package net.focik.homeoffice.library.domain.checker;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
            // API BookBeat nie łączy tytułu z autorem w jednym zapytaniu ("tytuł autor" -> 0 wyników),
            // więc szukamy samym tytułem, a autora porównujemy w wynikach.
            String apiUrl = UriComponentsBuilder.fromUriString("https://search-api.bookbeat.com/api/appsearch/books")
                    .queryParam("query", title)
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
                String fallbackUrl = null;
                for (JsonNode book : books) {
                    String bookTitle = book.path("title").asString("");
                    if (titleMatches(bookTitle, title) && hasAudiobook(book)) {
                        String shareUrl = book.path("shareurl").asString("");
                        if (shareUrl.isEmpty()) {
                            continue;
                        }
                        if (authorMatches(book.path("author").asString(""), author)) {
                            return AudiobookPlatformResult.available(bookstore.getId(), "Bookbeat", shareUrl);
                        }
                        if (fallbackUrl == null) {
                            fallbackUrl = shareUrl;
                        }
                    }
                }
                // tytuł pasuje, ale autor zapisany inaczej - lepiej pokazać niż ukryć
                if (fallbackUrl != null) {
                    return AudiobookPlatformResult.available(bookstore.getId(), "Bookbeat", fallbackUrl);
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
        return audiobookIsbn != null && !audiobookIsbn.isNull() && !audiobookIsbn.asString("").isEmpty();
    }

    private boolean authorMatches(String foundAuthor, String expectedAuthor) {
        if (expectedAuthor == null || expectedAuthor.isBlank()) {
            return true;
        }
        String found = foundAuthor.toLowerCase();
        for (String part : expectedAuthor.toLowerCase().split("\\s+")) {
            if (part.length() > 2 && found.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean titleMatches(String found, String expected) {
        return found.toLowerCase().contains(expected.toLowerCase());
    }
}
