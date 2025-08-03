package net.focik.homeoffice.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BookStatisticDto {
    private Integer year;
    private Long audiobook;
    private Long book;
    private Long ebook;
}
