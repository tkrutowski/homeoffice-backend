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

}
