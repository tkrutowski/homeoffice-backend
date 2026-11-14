package net.focik.homeoffice.library.api.dto;

import java.util.List;

/**
 * Odpowiedź na wolny tekst. {@code intent} mówi, którą część czytać:
 * <ul>
 *   <li>{@code FIND_BOOK}: {@code candidates} i {@code bestMatch} ({@code series} jest null),</li>
 *   <li>{@code SERIES_ALL} / {@code SERIES_UNREAD} / {@code SERIES_NEXT}: {@code series} ({@code candidates} jest puste).</li>
 * </ul>
 * {@code query} to to, co zrozumieliśmy z tekstu; {@code editionType} i {@code ownershipStatus} to nazwy enumów lub null.
 * {@code bestMatch} to jeden z {@code candidates}, który można zaznaczyć bez pytania użytkownika, albo null,
 * gdy wynik jest niejednoznaczny (wtedy użytkownik wybiera z listy).
 */
public record BookTextSearchResponseDto(
        String intent,
        Query query,
        List<BookSearchResultDto> candidates,
        BookSearchResultDto bestMatch,
        Series series) {

    public record Query(String title, String author, String editionType, String ownershipStatus, String series) {
    }

    /**
     * {@code status}: OK, SERIES_NAME_MISSING, SERIES_NOT_FOUND, AMBIGUOUS_SERIES (wtedy {@code options}),
     * NO_SUPPORTED_SOURCE, SOURCE_UNAVAILABLE, NOT_ORDERED, NO_NEXT_VOLUME (wtedy {@code lastRead}).
     * {@code volumes}: wszystkie lub nieprzeczytane tomy; {@code next}/{@code lastRead}: dla SERIES_NEXT.
     */
    public record Series(
            String status,
            String title,
            String url,
            List<String> options,
            List<Volume> volumes,
            Volume lastRead,
            Volume next) {
    }

    /** {@code status}: READ, READING, ON_SHELF lub NOT_IN_LIBRARY; {@code url} i {@code bookId} mogą być null. */
    public record Volume(
            String number,
            String title,
            String authors,
            String cover,
            String url,
            String status,
            Integer bookId) {
    }
}
