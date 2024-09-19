package net.focik.homeoffice.library.domain.scraper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.focik.homeoffice.library.domain.model.BookDto;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class BookScraperDto implements BookDto {
    private Integer id;
    private String series;
    private String seriesURL;
    private String authors;
    private String categories;
    private String title;
    private String description;
    private String cover;
    private String bookInSeriesNo;

    public static BookScraperDto createEmptyDto(){
        return BookScraperDto.builder()
                .authors("")
                .title("")
                .series("")
                .categories("")
                .build();
    }
}