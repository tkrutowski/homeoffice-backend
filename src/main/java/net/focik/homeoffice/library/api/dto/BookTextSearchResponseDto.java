package net.focik.homeoffice.library.api.dto;

import java.util.List;

/**
 * {@code query} to to, co zrozumieliśmy z tekstu; {@code editionType} i {@code ownershipStatus} to nazwy enumów lub null.
 * {@code bestMatch} to jeden z {@code candidates}, który można zaznaczyć bez pytania użytkownika, albo null,
 * gdy wynik jest niejednoznaczny (wtedy użytkownik wybiera z listy).
 */
public record BookTextSearchResponseDto(Query query, List<BookSearchResultDto> candidates, BookSearchResultDto bestMatch) {

    public record Query(String title, String author, String editionType, String ownershipStatus) {
    }
}
