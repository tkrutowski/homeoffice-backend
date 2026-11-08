package net.focik.homeoffice.library.domain;

import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.port.secondary.BookSearchPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookSearchServiceTest {

    private static BookSearchResult result(String title, String authors, String source) {
        return new BookSearchResult(title, authors, null, "https://x/" + source + "/" + title, source, null, null);
    }

    private static BookSearchPort port(String name, List<BookSearchResult> results) {
        return new BookSearchPort() {
            public String sourceName() { return name; }
            public List<BookSearchResult> search(String title, String author) { return results; }
        };
    }

    private static BookSearchPort failingPort() {
        return new BookSearchPort() {
            public String sourceName() { return "broken"; }
            public List<BookSearchResult> search(String title, String author) {
                throw new IllegalStateException("403 Cloudflare");
            }
        };
    }

    @Test
    void blankTitleReturnsEmptyWithoutCallingSources() {
        BookSearchService service = new BookSearchService(List.of(failingPort()));

        assertTrue(service.searchBooks("  ", "Chmielarz").isEmpty());
        assertTrue(service.searchBooks(null, null).isEmpty());
    }

    @Test
    void failingSourceDoesNotBreakOthers() {
        BookSearchService service = new BookSearchService(List.of(
                failingPort(),
                port("a", List.of(result("Rodzinny interes", "Wojciech Chmielarz", "a")))));

        List<BookSearchResult> found = service.searchBooks("Rodzinny interes", "Chmielarz");

        assertEquals(1, found.size());
        assertEquals("a", found.get(0).source());
    }

    @Test
    void exactTitleAndAuthorOutranksPartialMatch() {
        BookSearchService service = new BookSearchService(List.of(port("a", List.of(
                result("Rodzinny interes. Wydanie specjalne", "Ktoś Inny", "a"),
                result("Rodzinny interes", "Wojciech Chmielarz", "a")))));

        List<BookSearchResult> found = service.searchBooks("Rodzinny interes", "Chmielarz");

        assertEquals("Rodzinny interes", found.get(0).title());
    }

    @Test
    void matchingIgnoresCaseAndPolishDiacritics() {
        BookSearchService service = new BookSearchService(List.of(port("a", List.of(
                result("Inna książka", "Ktoś", "a"),
                result("Żółć", "Łukasz Źródło", "a")))));

        List<BookSearchResult> found = service.searchBooks("zolc", "lukasz");

        assertEquals("Żółć", found.get(0).title());
    }

    @Test
    void duplicateFromTwoSourcesKeepsFirstSource() {
        BookSearchService service = new BookSearchService(List.of(
                port("first", List.of(result("Rodzinny interes", "Wojciech Chmielarz", "first"))),
                port("second", List.of(result("rodzinny interes", "wojciech chmielarz", "second")))));

        List<BookSearchResult> found = service.searchBooks("Rodzinny interes", "");

        assertEquals(1, found.size());
        assertEquals("first", found.get(0).source());
    }

    @Test
    void limitsResultsToTen() {
        List<BookSearchResult> many = IntStream.range(0, 25)
                .mapToObj(i -> result("Tytuł " + i, "Autor", "a"))
                .toList();
        BookSearchService service = new BookSearchService(List.of(port("a", many)));

        assertEquals(10, service.searchBooks("Tytuł", "").size());
    }
}
