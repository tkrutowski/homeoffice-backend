package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.model.EditionType;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.UserBook;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public interface UserBookRepository {

    UserBook add(UserBook userBook);

    UserBook edit(UserBook userBook);

    boolean delete(Integer id);

    List<UserBook> findAllByIdBook(Integer idBook);

    List<UserBook> findAllByUserAndReadStatus(Long idUser, ReadingStatus readingStatus);

    List<UserBook> findAllByUser(Long idUser);
    List<UserBook> findAllByUserAndReadStatusAndYear(Long idUser, ReadingStatus readingStatus, LocalDate startDate, LocalDate stopDate );

    List<UserBook> findAllByUserAndAuthor(Long id, String query);

    List<Integer> findDistinctReadToYearsByUserId(Long id);

    Long countBooksByUserIdAndYearAndEditionType(Long id, Integer year, EditionType editionType);

    Optional<UserBook> findById(Integer id);

}
