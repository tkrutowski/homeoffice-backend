package net.focik.homeoffice.library.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import net.focik.homeoffice.library.domain.model.BookDto;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public
class UserBookApiDto implements BookDto {

    private Integer id;
    private Integer idUser;
    private BookApiDto book;
    private Integer idBookstore;
    private EditionTypeDto editionType;
    private ReadingStatusDto readingStatus;
    private OwnershipStatusDto ownershipStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate readFrom;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate readTo;
    private String info;
}
