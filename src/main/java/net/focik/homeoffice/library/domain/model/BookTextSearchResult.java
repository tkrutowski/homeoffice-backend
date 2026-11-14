package net.focik.homeoffice.library.domain.model;

import java.util.List;

/**
 * Wynik wyszukiwania z wolnego tekstu: to, co zrozumieliśmy ({@code query}, żeby użytkownik mógł
 * to zweryfikować) oraz kandydaci znalezieni na jego podstawie.
 *
 * @param bestMatch kandydat, którego możemy uznać za pewny (jeden z {@code candidates}), albo {@code null},
 *                  gdy wynik jest niejednoznaczny i użytkownik powinien wybrać z listy
 */
public record BookTextSearchResult(BookQuery query, List<BookSearchResult> candidates, BookSearchResult bestMatch) {

    public BookTextSearchResult(BookQuery query, List<BookSearchResult> candidates) {
        this(query, candidates, null);
    }
}
