package net.focik.homeoffice.library.api.mapper;

import net.focik.homeoffice.library.api.dto.BookSearchResultDto;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
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
}
