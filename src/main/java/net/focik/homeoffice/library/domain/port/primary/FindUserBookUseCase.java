package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.BookStatisticDto;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.UserBook;

import java.util.List;

public interface FindUserBookUseCase {
    List<UserBook> findUserBooksForBookId(Integer idBook, String userName);
    List<UserBook> findUserBooksByUser(String userName);

    UserBook findUserBook(Integer idUserBook);

    List<UserBook> findBookByUserAndReadStatus(String userName, ReadingStatus readingStatus);

    List<UserBook> findBookByUserAndReadStatusAndYear(String userName, ReadingStatus readingStatus, int year);

    List<BookStatisticDto> getStatistics(String userName);
}
