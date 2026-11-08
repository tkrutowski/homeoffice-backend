package net.focik.homeoffice.library.api.dto;

public record BookSearchResultDto(
        String title,
        String authors,
        String cover,
        String url,
        String source,
        String series,
        String bookInSeriesNo) {
}
