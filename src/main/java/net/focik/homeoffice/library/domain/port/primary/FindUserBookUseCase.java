package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.BookStatisticDto;
import net.focik.homeoffice.library.domain.model.Bookstore;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.UserBook;

import java.util.List;
import java.util.Map;

public interface FindUserBookUseCase {
    List<UserBook> findUserBooksForBookId(Integer idBook, String userName);
    List<UserBook> findUserBooksByUser(String userName);

    UserBook findUserBook(Integer idUserBook);

    List<UserBook> findBookByUserAndReadStatus(String userName, ReadingStatus readingStatus);

    List<UserBook> findUserBooksByQuery(String userName, String query);

    List<UserBook> findBookByUserAndReadStatusAndYear(String userName, ReadingStatus readingStatus, int year);

    List<BookStatisticDto> getStatistics(String userName);

    Map<Bookstore, Long> getStatisticsBookstore(String userName);
}
