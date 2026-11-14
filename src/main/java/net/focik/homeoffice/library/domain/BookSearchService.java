package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookQuery;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.model.BookTextSearchResult;
import net.focik.homeoffice.library.domain.model.SeriesAnswer;
import net.focik.homeoffice.library.domain.model.SeriesAnswerStatus;
import net.focik.homeoffice.library.domain.port.primary.SearchBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.SearchSeriesUseCase;
import net.focik.homeoffice.library.domain.port.secondary.BookQueryExtractorPort;
import net.focik.homeoffice.library.domain.port.secondary.BookSearchPort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
class BookSearchService implements SearchBookUseCase {

    private static final int MAX_RESULTS = 10;
    // punktacja dopasowania; dokładny tytuł > tytuł zawierający frazę, autor tylko dokłada punkt
    private static final int TITLE_EXACT = 4;
    private static final int TITLE_CONTAINS = 2;
    private static final int AUTHOR_MATCH = 1;

    private final List<BookSearchPort> searchPorts;
    private final BookQueryExtractorPort queryExtractor;
    private final SearchSeriesUseCase searchSeriesUseCase;

    @Override
    public BookTextSearchResult searchBooksByText(String text, String userName) {
        if (text == null || text.isBlank()) {
            return new BookTextSearchResult(BookQuery.empty(), List.of());
        }
        BookQuery query = queryExtractor.extract(text.trim());
        if (query.intent().isSeries()) {
            // pytanie o cykl: odpowiada serwis cykli, kandydatów książek nie ma
            SeriesAnswer answer = query.hasSeries()
                    ? searchSeriesUseCase.answer(query.intent(), query.series(), userName)
                    : SeriesAnswer.of(SeriesAnswerStatus.SERIES_NAME_MISSING);
            return new BookTextSearchResult(query, List.of(), null, answer);
        }
        if (!query.hasTitle()) {
            log.debug("No title recognized in text '{}'", text);
            return new BookTextSearchResult(query, List.of());
        }
        String title = query.title().trim();
        String author = query.author() == null ? "" : query.author().trim();
        List<BookSearchResult> candidates = searchBooks(title, author);
        return new BookTextSearchResult(query, candidates, findBestMatch(candidates, title, author));
    }

    /**
     * Zwraca kandydata, którego można wybrać bez pytania użytkownika, albo null. Jest "pewny" tylko gdy
     * ma dokładny tytuł, pasującego autora (jeśli go podano) i <b>ściśle</b> wyprzedza drugą pozycję.
     * Bez autora "Rodzinny interes" jest więc niejednoznaczny (są trzy takie książki), a z autorem już nie.
     * Literówka w nazwisku też daje null - wtedy lepiej pokazać listę niż zgadywać.
     */
    private BookSearchResult findBestMatch(List<BookSearchResult> ranked, String title, String author) {
        if (ranked.isEmpty()) {
            return null;
        }
        int required = author.isBlank() ? TITLE_EXACT : TITLE_EXACT + AUTHOR_MATCH;
        int top = score(ranked.get(0), title, author);
        int second = ranked.size() > 1 ? score(ranked.get(1), title, author) : -1;
        return top >= required && top > second ? ranked.get(0) : null;
    }

    @Override
    public List<BookSearchResult> searchBooks(String title, String author) {
        if (title == null || title.isBlank()) {
            return List.of();
        }
        String cleanTitle = title.trim();
        String cleanAuthor = author == null ? "" : author.trim();

        // dedupe po (tytuł, pierwszy autor); przy duplikacie wygrywa źródło wcześniejsze w kolejności portów
        Map<String, BookSearchResult> unique = new LinkedHashMap<>();
        for (BookSearchPort port : searchPorts) {
            try {
                List<BookSearchResult> found = port.search(cleanTitle, cleanAuthor);
                log.debug("Book search '{}' / '{}' in {}: {} results", cleanTitle, cleanAuthor, port.sourceName(), found.size());
                for (BookSearchResult result : found) {
                    unique.putIfAbsent(dedupeKey(result), result);
                }
            } catch (Exception e) {
                // jedno niedostępne/zablokowane źródło nie może zepsuć całego wyszukiwania
                log.warn("Book search source {} failed: {}", port.sourceName(), e.getMessage());
            }
        }

        List<BookSearchResult> ranked = new ArrayList<>(unique.values());
        // sort jest stabilny - przy równym wyniku zostaje kolejność trafności ze źródła
        ranked.sort(Comparator.comparingInt((BookSearchResult r) -> score(r, cleanTitle, cleanAuthor)).reversed());
        return ranked.size() > MAX_RESULTS ? List.copyOf(ranked.subList(0, MAX_RESULTS)) : ranked;
    }

    private int score(BookSearchResult result, String title, String author) {
        int score = 0;
        String foundTitle = normalize(result.title());
        String expectedTitle = normalize(title);
        if (foundTitle.equals(expectedTitle)) {
            score += TITLE_EXACT;
        } else if (foundTitle.contains(expectedTitle)) {
            score += TITLE_CONTAINS;
        }
        if (!author.isBlank() && authorMatches(result.authors(), author)) {
            score += AUTHOR_MATCH;
        }
        return score;
    }

    private boolean authorMatches(String foundAuthors, String expectedAuthor) {
        String found = normalize(foundAuthors);
        for (String part : normalize(expectedAuthor).split("\\s+")) {
            if (part.length() > 2 && found.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private String dedupeKey(BookSearchResult result) {
        String firstAuthor = result.authors() == null ? "" : result.authors().split(",")[0];
        return normalize(result.title()) + "|" + normalize(firstAuthor);
    }

    private String normalize(String text) {
        return SearchText.normalize(text);
    }
}
