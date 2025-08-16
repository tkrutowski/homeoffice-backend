package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.infrastructure.dto.BookDbDto;
import net.focik.homeoffice.library.infrastructure.dto.SeriesDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface BookDtoRepository extends JpaRepository<BookDbDto, Integer> {

    Iterable<BookDbDto> findAllByTitleIgnoreCase(String title);

    List<BookDbDto> findAllByOrderByTitleAsc();

    List<BookDbDto> findAllByOrderByIdDesc();

    List<BookDbDto> findAllBySeriesOrderByBookInSeriesNo(SeriesDbDto seriesDbDto);
}
