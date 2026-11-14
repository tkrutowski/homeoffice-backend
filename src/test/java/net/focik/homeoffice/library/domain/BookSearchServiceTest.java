package net.focik.homeoffice.library.domain;

import net.focik.homeoffice.library.domain.exception.BookQueryExtractionException;
import net.focik.homeoffice.library.domain.model.BookQuery;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.model.BookTextSearchResult;
import net.focik.homeoffice.library.domain.model.EditionType;
import net.focik.homeoffice.library.domain.model.OwnershipStatus;
import net.focik.homeoffice.library.domain.port.secondary.BookQueryExtractorPort;
import net.focik.homeoffice.library.domain.port.secondary.BookSearchPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /** Ekstraktor, który nie powinien być wołany (testy wyszukiwania po tytule/autorze). */
    private static BookQueryExtractorPort unusedExtractor() {
        return text -> {
            throw new AssertionError("extractor should not be called");
        };
    }

    private static BookSearchService service(BookSearchPort... ports) {
        return new BookSearchService(List.of(ports), unusedExtractor());
    }

    @Test
    void blankTitleReturnsEmptyWithoutCallingSources() {
        BookSearchService service = service(failingPort());

        assertTrue(service.searchBooks("  ", "Chmielarz").isEmpty());
        assertTrue(service.searchBooks(null, null).isEmpty());
    }

    @Test
    void failingSourceDoesNotBreakOthers() {
        BookSearchService service = service(
                failingPort(),
                port("a", List.of(result("Rodzinny interes", "Wojciech Chmielarz", "a"))));

        List<BookSearchResult> found = service.searchBooks("Rodzinny interes", "Chmielarz");

        assertEquals(1, found.size());
        assertEquals("a", found.get(0).source());
    }

    @Test
    void exactTitleAndAuthorOutranksPartialMatch() {
        BookSearchService service = service(port("a", List.of(
                result("Rodzinny interes. Wydanie specjalne", "Ktoś Inny", "a"),
                result("Rodzinny interes", "Wojciech Chmielarz", "a"))));

        List<BookSearchResult> found = service.searchBooks("Rodzinny interes", "Chmielarz");

        assertEquals("Rodzinny interes", found.get(0).title());
    }

    @Test
    void matchingIgnoresCaseAndPolishDiacritics() {
        BookSearchService service = service(port("a", List.of(
                result("Inna książka", "Ktoś", "a"),
                result("Żółć", "Łukasz Źródło", "a"))));

        List<BookSearchResult> found = service.searchBooks("zolc", "lukasz");

        assertEquals("Żółć", found.get(0).title());
    }

    @Test
    void duplicateFromTwoSourcesKeepsFirstSource() {
        BookSearchService service = service(
                port("first", List.of(result("Rodzinny interes", "Wojciech Chmielarz", "first"))),
                port("second", List.of(result("rodzinny interes", "wojciech chmielarz", "second"))));

        List<BookSearchResult> found = service.searchBooks("Rodzinny interes", "");

        assertEquals(1, found.size());
        assertEquals("first", found.get(0).source());
    }

    @Test
    void limitsResultsToTen() {
        List<BookSearchResult> many = IntStream.range(0, 25)
                .mapToObj(i -> result("Tytuł " + i, "Autor", "a"))
                .toList();
        BookSearchService service = service(port("a", many));

        assertEquals(10, service.searchBooks("Tytuł", "").size());
    }

    // --- wyszukiwanie z wolnego tekstu ---

    /** Ekstraktor zwracający z góry zadane zapytanie i zapamiętujący, z jakim tekstem go wołano. */
    private static class FakeExtractor implements BookQueryExtractorPort {
        final List<String> calls = new ArrayList<>();
        private final BookQuery query;

        FakeExtractor(BookQuery query) {
            this.query = query;
        }

        public BookQuery extract(String text) {
            calls.add(text);
            return query;
        }
    }

    @Test
    void textSearchSearchesWithExtractedTitleAndAuthorAndReturnsQuery() {
        BookQuery query = new BookQuery("Rodzinny interes", "Chmielarz", EditionType.AUDIOBOOK, OwnershipStatus.WONT);
        FakeExtractor extractor = new FakeExtractor(query);
        BookSearchService service = new BookSearchService(
                List.of(port("a", List.of(
                        result("Coś innego", "Ktoś", "a"),
                        result("Rodzinny interes", "Wojciech Chmielarz", "a")))),
                extractor);

        BookTextSearchResult found = service.searchBooksByText("  znajdź mi audiobook rodzinny interes autora chmielarz ");

        assertSame(query, found.query());
        assertEquals("Rodzinny interes", found.candidates().get(0).title());
        assertSame(found.candidates().get(0), found.bestMatch());
        assertEquals(List.of("znajdź mi audiobook rodzinny interes autora chmielarz"), extractor.calls);
    }

    @Test
    void textSearchWithBlankTextDoesNotCallExtractor() {
        BookSearchService service = new BookSearchService(List.of(failingPort()), unusedExtractor());

        BookTextSearchResult found = service.searchBooksByText("   ");

        assertTrue(found.candidates().isEmpty());
        assertTrue(!found.query().hasTitle());
        assertTrue(service.searchBooksByText(null).candidates().isEmpty());
    }

    @Test
    void textSearchWithoutRecognizedTitleReturnsNoCandidatesAndSkipsSources() {
        BookQuery noTitle = new BookQuery(null, "Chmielarz", null, null);
        BookSearchService service = new BookSearchService(
                List.of(new BookSearchPort() {
                    public String sourceName() { return "must-not-be-called"; }
                    public List<BookSearchResult> search(String title, String author) {
                        throw new AssertionError("sources should not be called without a title");
                    }
                }),
                new FakeExtractor(noTitle));

        BookTextSearchResult found = service.searchBooksByText("coś tam autora chmielarz");

        assertSame(noTitle, found.query());
        assertTrue(found.candidates().isEmpty());
    }

    @Test
    void textSearchPropagatesExtractionFailure() {
        BookSearchService service = new BookSearchService(List.of(port("a", List.of())), text -> {
            throw new BookQueryExtractionException("Nie udało się zrozumieć zapytania", new RuntimeException("AI down"));
        });

        assertThrows(BookQueryExtractionException.class, () -> service.searchBooksByText("cokolwiek"));
    }

    // --- bestMatch ---

    private static final BookSearchResult CHMIELARZ = result("Rodzinny interes", "Wojciech Chmielarz", "a");
    private static final BookSearchResult ROSLUND = result("Rodzinny interes", "Anders Roslund, Stefan Thunberg", "a");
    private static final BookSearchResult AVENGERS = result("Avengers: Rodzinny interes", "Mark Waid", "a");

    private static BookTextSearchResult textSearch(String title, String author, BookSearchResult... found) {
        BookSearchService service = new BookSearchService(
                List.of(port("a", List.of(found))),
                new FakeExtractor(new BookQuery(title, author, null, null)));
        return service.searchBooksByText("dowolny tekst");
    }

    @Test
    void bestMatchIsSetWhenExactTitleAndAuthorMatchAndNextIsWorse() {
        BookTextSearchResult found = textSearch("Rodzinny interes", "Chmielarz", AVENGERS, ROSLUND, CHMIELARZ);

        assertSame(CHMIELARZ, found.bestMatch());
        assertSame(CHMIELARZ, found.candidates().get(0));
        assertEquals(3, found.candidates().size());
    }

    @Test
    void bestMatchIsNullWithoutAuthorWhenSeveralBooksHaveTheExactTitle() {
        // "Rodzinny interes" bez autora to dwie różne książki - to użytkownik ma wybrać
        BookTextSearchResult found = textSearch("Rodzinny interes", null, CHMIELARZ, ROSLUND, AVENGERS);

        assertNull(found.bestMatch());
        assertEquals(3, found.candidates().size());
    }

    @Test
    void bestMatchIsSetWithoutAuthorWhenExactTitleIsUnique() {
        BookTextSearchResult found = textSearch("Rodzinny interes", null, AVENGERS, CHMIELARZ);

        assertSame(CHMIELARZ, found.bestMatch());
    }

    @Test
    void bestMatchIsNullWhenAuthorGivenButDoesNotMatchTopResult() {
        // literówka w nazwisku: lepiej pokazać listę niż zgadywać
        BookTextSearchResult found = textSearch("Rodzinny interes", "Chmielasz", CHMIELARZ, AVENGERS);

        assertNull(found.bestMatch());
        assertEquals(2, found.candidates().size());
    }

    @Test
    void bestMatchIsNullWhenTitleIsOnlyContainedNotExact() {
        BookTextSearchResult found = textSearch("Rodzinny interes", null, AVENGERS);

        assertNull(found.bestMatch());
    }

    @Test
    void bestMatchIsNullWhenNothingFoundOrNoTitleRecognized() {
        assertNull(textSearch("Nieistniejąca książka", "Nikt").bestMatch());
        assertNull(textSearch(null, "Chmielarz", CHMIELARZ).bestMatch());
    }
}
