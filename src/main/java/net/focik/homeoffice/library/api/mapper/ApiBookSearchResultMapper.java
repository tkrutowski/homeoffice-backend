package net.focik.homeoffice.library.api.mapper;

import net.focik.homeoffice.library.api.dto.BookSearchResultDto;
import net.focik.homeoffice.library.api.dto.BookTextSearchResponseDto;
import net.focik.homeoffice.library.domain.model.BookQuery;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.model.BookTextSearchResult;
import net.focik.homeoffice.library.domain.model.SeriesAnswer;
import net.focik.homeoffice.library.domain.model.SeriesVolume;
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
                query.intent().name(),
                new BookTextSearchResponseDto.Query(
                        query.title(),
                        query.author(),
                        query.editionType() == null ? null : query.editionType().name(),
                        query.ownershipStatus() == null ? null : query.ownershipStatus().name(),
                        query.series()),
                result.candidates().stream().map(this::toDto).toList(),
                result.bestMatch() == null ? null : toDto(result.bestMatch()),
                result.series() == null ? null : toDto(result.series()));
    }

    private BookTextSearchResponseDto.Series toDto(SeriesAnswer answer) {
        return new BookTextSearchResponseDto.Series(
                answer.status().name(),
                answer.seriesTitle(),
                answer.seriesUrl(),
                answer.options(),
                answer.volumes().stream().map(this::toDto).toList(),
                answer.lastRead() == null ? null : toDto(answer.lastRead()),
                answer.next() == null ? null : toDto(answer.next()));
    }

    private BookTextSearchResponseDto.Volume toDto(SeriesVolume volume) {
        return new BookTextSearchResponseDto.Volume(
                volume.number(),
                volume.title(),
                volume.authors(),
                volume.cover(),
                volume.url(),
                volume.status().name(),
                volume.bookId());
    }
}
