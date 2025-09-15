package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.infrastructure.dto.BookDbDto;
import net.focik.homeoffice.library.infrastructure.dto.SeriesDbDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface BookDtoRepository extends JpaRepository<BookDbDto, Integer> {

    Iterable<BookDbDto> findAllByTitleIgnoreCase(String title);

    List<BookDbDto> findAllByOrderByTitleAsc();

    List<BookDbDto> findAllByOrderByIdDesc();

    List<BookDbDto> findAllBySeriesOrderByBookInSeriesNo(SeriesDbDto seriesDbDto);

    @Query("SELECT DISTINCT b FROM BookDbDto b " +
            "LEFT JOIN FETCH b.authors a " +
            "LEFT JOIN FETCH b.categories c " +
            "LEFT JOIN FETCH b.series s " +
            "WHERE (:globalFilter IS NULL OR " +
            "       LOWER(b.title) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "       LOWER(a.firstName) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "       LOWER(a.lastName) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "       LOWER(c.name) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "       LOWER(s.title) LIKE LOWER(CONCAT('%', :globalFilter, '%'))) " +
            "AND (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:author IS NULL OR LOWER(CONCAT(a.lastName, ' ', a.firstName)) LIKE LOWER(CONCAT('%', :author, '%'))) " +
            "AND (:category IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :category, '%'))) " +
            "AND (:series IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :series, '%')))")
    Page<BookDbDto> findBooksWithFilters(
            @Param("globalFilter") String globalFilter,
            @Param("title") String title,
            @Param("author") String author,
            @Param("category") String category,
            @Param("series") String series,
            Pageable pageable
    );
}
