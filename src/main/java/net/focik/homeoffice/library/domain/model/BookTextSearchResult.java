package net.focik.homeoffice.library.domain.model;

import java.util.List;

/**
 * Wynik wyszukiwania z wolnego tekstu: to, co zrozumieliśmy ({@code query}, żeby użytkownik mógł
 * to zweryfikować) oraz odpowiedź zależna od intencji: kandydaci dla {@code FIND_BOOK} albo
 * {@code series} dla pytań o cykl (wtedy {@code candidates} jest puste).
 *
 * @param bestMatch kandydat, którego możemy uznać za pewny (jeden z {@code candidates}), albo {@code null},
 *                  gdy wynik jest niejednoznaczny i użytkownik powinien wybrać z listy
 * @param series    odpowiedź na pytanie o cykl; {@code null} dla {@code FIND_BOOK}
 */
public record BookTextSearchResult(
        BookQuery query,
        List<BookSearchResult> candidates,
        BookSearchResult bestMatch,
        SeriesAnswer series) {

    public BookTextSearchResult(BookQuery query, List<BookSearchResult> candidates) {
        this(query, candidates, null, null);
    }

    public BookTextSearchResult(BookQuery query, List<BookSearchResult> candidates, BookSearchResult bestMatch) {
        this(query, candidates, bestMatch, null);
    }
}
