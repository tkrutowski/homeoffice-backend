package net.focik.homeoffice.library.api.mapper;

import net.focik.homeoffice.library.api.dto.BookSearchResultDto;
import net.focik.homeoffice.library.api.dto.BookTextSearchResponseDto;
import net.focik.homeoffice.library.domain.model.BookQuery;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.model.BookTextSearchResult;
import org.springframework.stereotype.Component;

@Component
public class ApiBookSearchResultMapper {

    public BookSearchResultDto toDto(BookSearchResult result) {
        return new BookSearchResultDto(
                result.title(),
                result.authors(),
                result.cover(),
                result.url(),
                result.source(),
                result.series(),
                result.bookInSeriesNo());
    }

    public BookTextSearchResponseDto toDto(BookTextSearchResult result) {
        BookQuery query = result.query();
        return new BookTextSearchResponseDto(
                new BookTextSearchResponseDto.Query(
                        query.title(),
                        query.author(),
                        query.editionType() == null ? null : query.editionType().name(),
                        query.ownershipStatus() == null ? null : query.ownershipStatus().name()),
                result.candidates().stream().map(this::toDto).toList(),
                result.bestMatch() == null ? null : toDto(result.bestMatch()));
    }
}
