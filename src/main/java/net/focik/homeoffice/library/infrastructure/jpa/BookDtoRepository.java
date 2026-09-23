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
            "JOIN b.authors a " +
            "LEFT JOIN b.series s " +
            "WHERE a.id = :authorId " +
            "ORDER BY s.title, CAST(NULLIF(b.bookInSeriesNo, '') AS int)")
    List<BookDbDto> findAllByAuthorIdOrderByTitle(@Param("authorId") Integer authorId);


    /**
     * Krok 1 paginacji: strona samych id książek. Filtry po kolekcjach (autorzy, kategorie) idą przez EXISTS,
     * więc nie ma JOIN FETCH kolekcji razem z LIMIT (Hibernate przenosiłby wtedy WHERE do podzapytania).
     */
    @Query(value = "SELECT b.id FROM BookDbDto b " +
            "LEFT JOIN b.series s " +
            "WHERE (:globalFilter IS NULL OR " +
            "       LOWER(b.title) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "       LOWER(s.title) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "       EXISTS (SELECT 1 FROM b.authors ga WHERE " +
            "               LOWER(ga.firstName) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "               LOWER(ga.lastName) LIKE LOWER(CONCAT('%', :globalFilter, '%'))) OR " +
            "       EXISTS (SELECT 1 FROM b.categories gc WHERE " +
            "               LOWER(gc.name) LIKE LOWER(CONCAT('%', :globalFilter, '%')))) " +
            "AND (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:author IS NULL OR EXISTS (SELECT 1 FROM b.authors fa WHERE " +
            "        LOWER(CONCAT(fa.lastName, ' ', fa.firstName)) LIKE LOWER(CONCAT('%', :author, '%')))) " +
            "AND (:category IS NULL OR EXISTS (SELECT 1 FROM b.categories fc WHERE " +
            "        LOWER(fc.name) LIKE LOWER(CONCAT('%', :category, '%')))) " +
            "AND (:series IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :series, '%')))",
            countQuery = "SELECT COUNT(b) FROM BookDbDto b " +
                    "LEFT JOIN b.series s " +
                    "WHERE (:globalFilter IS NULL OR " +
                    "       LOWER(b.title) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
                    "       LOWER(s.title) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
                    "       EXISTS (SELECT 1 FROM b.authors ga WHERE " +
                    "               LOWER(ga.firstName) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
                    "               LOWER(ga.lastName) LIKE LOWER(CONCAT('%', :globalFilter, '%'))) OR " +
                    "       EXISTS (SELECT 1 FROM b.categories gc WHERE " +
                    "               LOWER(gc.name) LIKE LOWER(CONCAT('%', :globalFilter, '%')))) " +
                    "AND (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
                    "AND (:author IS NULL OR EXISTS (SELECT 1 FROM b.authors fa WHERE " +
                    "        LOWER(CONCAT(fa.lastName, ' ', fa.firstName)) LIKE LOWER(CONCAT('%', :author, '%')))) " +
                    "AND (:category IS NULL OR EXISTS (SELECT 1 FROM b.categories fc WHERE " +
                    "        LOWER(fc.name) LIKE LOWER(CONCAT('%', :category, '%')))) " +
                    "AND (:series IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :series, '%')))")
    Page<Integer> findBookIdsWithFilters(
            @Param("globalFilter") String globalFilter,
            @Param("title") String title,
            @Param("author") String author,
            @Param("category") String category,
            @Param("series") String series,
            Pageable pageable
    );

    /** Krok 2 paginacji: załadowanie encji z kolekcjami dla id ze strony (bez LIMIT, więc JOIN FETCH jest bezpieczny). */
    @Query("SELECT DISTINCT b FROM BookDbDto b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH b.categories " +
            "LEFT JOIN FETCH b.series " +
            "WHERE b.id IN :ids")
    List<BookDbDto> findAllWithDetailsByIdIn(@Param("ids") List<Integer> ids);

    @Query("SELECT COUNT(DISTINCT b) FROM BookDbDto b JOIN b.authors a WHERE a.id = :authorId")
    Long countBooksByAuthorId(Integer authorId);

    @Query("SELECT COUNT(DISTINCT b) FROM BookDbDto b JOIN b.categories c WHERE c.id = :categoryId")
    Long countBooksByCategoryId(Integer categoryId);

}
