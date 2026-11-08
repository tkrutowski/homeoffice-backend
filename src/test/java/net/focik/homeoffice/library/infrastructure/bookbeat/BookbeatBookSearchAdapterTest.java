package net.focik.homeoffice.library.infrastructure.bookbeat;

import net.focik.homeoffice.library.domain.model.BookSearchResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookbeatBookSearchAdapterTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    // Kształt prawdziwej odpowiedzi appsearch/books (skrócony) + wpisy bez serii i niekompletne
    private static final String JSON = """
            {"count":3,"_embedded":{"books":[
              {"id":1458975,"title":"Rodzinny interes","author":"Wojciech Chmielarz",
               "image":"https://prod-bb-images.akamaized.net/book-covers/x.jpg?w=400",
               "shareurl":"https://www.bookbeat.com/pl/book/1458975","audiobookisbn":"9788368226652",
               "series":{"id":87733,"name":"Bezimienny","partnumber":4,"displaypartnumber":"4"}},
              {"id":2,"title":"Bez serii","author":"Jan Kowalski","image":"",
               "shareurl":"https://www.bookbeat.com/pl/book/2","series":null},
              {"id":3,"title":"Bez linku","author":"Ktoś"},
              {"id":4,"title":"","shareurl":"https://www.bookbeat.com/pl/book/4"}
            ]}}
            """;

    @Test
    void parsesTitleAuthorCoverUrlAndSeries() {
        List<BookSearchResult> results = BookbeatBookSearchAdapter.parse(MAPPER.readTree(JSON));

        BookSearchResult first = results.get(0);
        assertEquals("Rodzinny interes", first.title());
        assertEquals("Wojciech Chmielarz", first.authors());
        assertEquals("https://prod-bb-images.akamaized.net/book-covers/x.jpg?w=400", first.cover());
        assertEquals("https://www.bookbeat.com/pl/book/1458975", first.url());
        assertEquals("Bezimienny", first.series());
        assertEquals("4", first.bookInSeriesNo());
        assertEquals("bookbeat", first.source());
    }

    @Test
    void handlesMissingSeriesAndCoverAndSkipsIncompleteEntries() {
        List<BookSearchResult> results = BookbeatBookSearchAdapter.parse(MAPPER.readTree(JSON));

        assertEquals(2, results.size());
        BookSearchResult noSeries = results.get(1);
        assertEquals("Bez serii", noSeries.title());
        assertNull(noSeries.cover());
        assertNull(noSeries.series());
        assertNull(noSeries.bookInSeriesNo());
    }

    @Test
    void returnsEmptyWhenNoBooksArray() {
        assertTrue(BookbeatBookSearchAdapter.parse(MAPPER.readTree("{\"count\":0,\"aggregations\":[]}")).isEmpty());
    }
}
