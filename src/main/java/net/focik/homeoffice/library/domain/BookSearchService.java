package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.port.primary.SearchBookUseCase;
import net.focik.homeoffice.library.domain.port.secondary.BookSearchPort;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
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

    private final List<BookSearchPort> searchPorts;

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
            score += 4;
        } else if (foundTitle.contains(expectedTitle)) {
            score += 2;
        }
        if (!author.isBlank() && authorMatches(result.authors(), author)) {
            score += 1;
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

    /** Małe litery, bez znaków diakrytycznych (ł nie rozkłada się w NFD, więc osobno). */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.toLowerCase().replace('ł', 'l'), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ").trim();
    }
}
