package net.focik.homeoffice.library.infrastructure.bookbeat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.port.secondary.BookSearchPort;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Wyszukiwanie w katalogu BookBeat (to samo API co {@code BookbeatAudiobookChecker}).
 * <p>
 * API szuka po tytule <b>albo</b> po autorze, ale nie po obu naraz ("tytuł autor" daje 0 wyników),
 * dlatego pytamy samym tytułem, a autora porównuje dopiero {@code BookSearchService}.
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class BookbeatBookSearchAdapter implements BookSearchPort {

    static final String SOURCE = "bookbeat";
    private static final String API_URL = "https://search-api.bookbeat.com/api/appsearch/books";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public List<BookSearchResult> search(String title, String author) {
        URI uri = UriComponentsBuilder.fromUriString(API_URL)
                .queryParam("query", title)
                .queryParam("market", "48")
                .queryParam("includeErotic", "false")
                .queryParam("limit", "20")
                .queryParam("offset", "0")
                .queryParam("v", "18")
                .build()
                .encode()
                .toUri();

        String response = restTemplate.getForObject(uri, String.class);
        if (response == null || response.isBlank()) {
            return List.of();
        }
        return parse(objectMapper.readTree(response));
    }

    static List<BookSearchResult> parse(JsonNode root) {
        List<BookSearchResult> results = new ArrayList<>();
        JsonNode books = root.path("_embedded").path("books");
        if (!books.isArray()) {
            return results;
        }
        for (JsonNode book : books) {
            String title = book.path("title").asString("").trim();
            String url = book.path("shareurl").asString("").trim();
            if (title.isEmpty() || url.isEmpty()) {
                continue;
            }
            String cover = book.path("image").asString("");
            JsonNode series = book.path("series");
            String seriesName = series.path("name").asString("");
            String partNumber = series.path("displaypartnumber").asString("");

            results.add(new BookSearchResult(
                    title,
                    book.path("author").asString("").trim(),
                    cover.isBlank() ? null : cover,
                    url,
                    SOURCE,
                    seriesName.isBlank() ? null : seriesName,
                    partNumber.isBlank() ? null : partNumber));
        }
        return results;
    }
}
