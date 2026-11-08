package net.focik.homeoffice.library.domain.model;

/**
 * Kandydat z wyszukiwania książki po tytule/autorze. To tylko skrót z listy wyników -
 * pełne dane (kategorie, opis) pobiera dopiero {@code findBookByUrl(url)}.
 */
public record BookSearchResult(
        String title,
        String authors,
        String cover,
        String url,
        String source,
        String series,
        String bookInSeriesNo) {
}
