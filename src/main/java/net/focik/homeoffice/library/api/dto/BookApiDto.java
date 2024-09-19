package net.focik.homeoffice.library.api.dto;

import lombok.*;
import net.focik.homeoffice.library.domain.model.BookDto;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BookApiDto implements BookDto {

    private Integer id;
    private SeriesDto series;
    private List<AuthorDto> authors;
    private List<CategoryDto> categories;
    private String title;
    private String description;
    private String cover;
    private String bookInSeriesNo;
}
