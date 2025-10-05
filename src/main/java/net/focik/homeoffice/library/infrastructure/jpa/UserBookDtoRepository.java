package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.domain.model.EditionType;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.infrastructure.dto.UserBookDbDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

interface UserBookDtoRepository extends CrudRepository<UserBookDbDto, Integer> {
    List<UserBookDbDto> findAllByBook_Id(Integer idBook);

    List<UserBookDbDto> findAllByUser_Id(Long idUser);

    List<UserBookDbDto> findAllByReadingStatusAndUser_Id(ReadingStatus readingStatus, Long idUser);

    @Query("SELECT COUNT(ub) FROM UserBookDbDto ub WHERE " +
            "ub.user.id = :userId AND " +
            "ub.editionType = :editionType AND " +
            "ub.readingStatus = 'READ' AND " +
            "YEAR(ub.readTo) = :year")
    Long countBooksByUserIdAndYearAndEditionType(Long userId, Integer year, EditionType editionType);

    List<UserBookDbDto> findAllByReadingStatusAndUser_IdAndReadToBetween(ReadingStatus readingStatus, Long idUser, LocalDate startDate, LocalDate endDate);


    List<UserBookDbDto> findAllByUser_IdAndBook_TitleContainingIgnoreCase(Long userId, String title);

    List<UserBookDbDto> findAllByUser_IdAndBook_Series_TitleContainingIgnoreCase(Long userId, String seriesTitle);

    List<UserBookDbDto> findAllByBookstore_Id(Integer idBookstore);

    @Query("SELECT DISTINCT ub FROM UserBookDbDto ub JOIN ub.book.authors a WHERE ub.user.id = :userId AND (LOWER(a.lastName) LIKE LOWER(CONCAT('%', :authorName, '%')) OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :authorName, '%')))")
    List<UserBookDbDto> findAllByUserIdAndAuthorName(Long userId, String authorName);

    @Query("SELECT DISTINCT YEAR(ub.readTo) FROM UserBookDbDto ub WHERE ub.user.id = :userId AND ub.readTo IS NOT NULL ORDER BY YEAR(ub.readTo) DESC")
    List<Integer> findDistinctReadToYearsByUserId(Long userId);

    @Query("SELECT COUNT(ub) FROM UserBookDbDto ub WHERE " +
            "ub.user.id = :userId AND " +
            "ub.bookstore.id = :bookstoreId AND " +
            "ub.readingStatus = 'READ'")
    Long countReadBooksByUserIdAndBookstoreId(Long userId, Integer bookstoreId);
}
