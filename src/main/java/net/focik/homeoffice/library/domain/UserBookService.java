package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.UserBookAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.UserBookNotFoundException;
import net.focik.homeoffice.library.domain.model.*;
import net.focik.homeoffice.library.domain.port.secondary.UserBookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
class UserBookService {

    /**
     * Statusy uznawane za "aktywną" pozycję na półce (książka nieukończona).
     * Dla danej książki i użytkownika może istnieć co najwyżej jedna pozycja
     * w jednym z tych statusów - status READ (Przeczytane) może się powtarzać
     * dowolną liczbę razy (np. wielokrotne czytanie tej samej książki).
     */
    private static final Set<ReadingStatus> ACTIVE_READING_STATUSES =
            EnumSet.of(ReadingStatus.NOT_READ, ReadingStatus.READ_NOW);

    private final UserBookRepository userBookRepository;

    public List<UserBook> findUserBooksForBookId(Integer idBook, Integer idUser) {
        List<UserBook> allByBook = userBookRepository.findAllByIdBook(idBook);
        return allByBook.stream()
                .filter(userBook -> idUser.equals(userBook.getUser().getId().intValue()))
                .collect(Collectors.toList());
    }

    public UserBook addUserBook(UserBook userBook) {
        if (ACTIVE_READING_STATUSES.contains(userBook.getReadingStatus())) {
            assertNoActiveDuplicate(userBook.getBook(), userBook.getUser().getId(), null);
        }
        return userBookRepository.add(userBook);
    }

    public UserBook updateUserBook(UserBook userBook) {
        log.debug("Trying to update userBook with id: {}", userBook.getId());
        Optional<UserBook> userBookById = userBookRepository.findById(userBook.getId());
        if (userBookById.isEmpty()) {
            log.warn("UserBook with id {} not found", userBook.getId());
            throw new UserBookNotFoundException(userBook.getId());
        }

        if (ACTIVE_READING_STATUSES.contains(userBook.getReadingStatus())) {
            assertNoActiveDuplicate(userBookById.get().getBook(), userBookById.get().getUser().getId(),
                    userBookById.get().getId());
        }

        userBookById.get().setBookstore(userBook.getBookstore());
        userBookById.get().setReadingStatus(userBook.getReadingStatus());
        userBookById.get().setEditionType(userBook.getEditionType());
        userBookById.get().setOwnershipStatus(userBook.getOwnershipStatus());
        userBookById.get().setReadFrom(userBook.getReadFrom());
        userBookById.get().setReadTo(userBook.getReadTo());
        userBookById.get().setInfo(userBook.getInfo());

        return userBookRepository.edit(userBookById.get());
    }

    /**
     * Sprawdza, czy użytkownik ma już inną pozycję tej samej książki w statusie
     * "W poczekalni" lub "Czytana" (patrz {@link #ACTIVE_READING_STATUSES}).
     * Jeśli tak, dodanie/edycja jest odrzucana - taka sama książka nie może
     * jednocześnie "czekać" i być "czytana" (to prowadzi do duplikatów na półce).
     *
     * @param excludeUserBookId id pozycji do pominięcia przy sprawdzaniu (przy edycji - sama siebie), albo null przy dodawaniu nowej
     */
    private void assertNoActiveDuplicate(Book book, Long idUser, Integer excludeUserBookId) {
        boolean duplicateExists = userBookRepository.findAllByIdBook(book.getId()).stream()
                .filter(ub -> !ub.getId().equals(excludeUserBookId))
                .filter(ub -> idUser.equals(ub.getUser().getId()))
                .anyMatch(ub -> ACTIVE_READING_STATUSES.contains(ub.getReadingStatus()));

        if (duplicateExists) {
            log.warn("UserBook duplicate rejected: book {} already on shelf for user {} in an active status", book.getId(), idUser);
            throw new UserBookAlreadyExistException(book);
        }
    }

    public void deleteUserBook(Integer id) {
        userBookRepository.delete(id);
    }

    public UserBook findUserBook(Integer id) {
        Optional<UserBook> userBookById = userBookRepository.findById(id);
        if (userBookById.isEmpty()) {
            log.warn("UserBook with id {} not found", id);
            return null;
        }
        return userBookById.get();
    }
    public List<UserBook> findUserBookByUser(Long idUser) {
        return userBookRepository.findAllByUser(idUser);
    }

    public List<UserBook> findBookByUserAndReadStatus(Long idUser, ReadingStatus readingStatus) {
        return userBookRepository.findAllByUserAndReadStatus(idUser, readingStatus);
    }

    public List<UserBook> findUserBooksByQuery(Long id, String query) {
        List<UserBook> allByUserAndTitle = userBookRepository.findAllByUserAndTitle(id, query);
        List<UserBook> allByUserAndSeries = userBookRepository.findAllByUserAndSeries(id, query);
        List<UserBook> allByUserAndAuthor = userBookRepository.findAllByUserAndAuthor(id, query);
        return Stream.of(allByUserAndTitle, allByUserAndSeries, allByUserAndAuthor
                )
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<UserBook> findBookByUserAndReadStatusAndYear(Long idUser, ReadingStatus readingStatus, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate stopDate = LocalDate.of(year, 12, 31);
        return userBookRepository.findAllByUserAndReadStatusAndYear(idUser, readingStatus, startDate, stopDate);
    }

    public List<BookStatisticDto> getStatistics(Long id) {
        List<Integer> distinctReadToYearsByUserId = userBookRepository.findDistinctReadToYearsByUserId(id);
        List<BookStatisticDto> bookStatisticDtos = new ArrayList<>();
        for (Integer year : distinctReadToYearsByUserId) {
            log.debug("Year: {}", year);
            Long bookCount = userBookRepository.countBooksByUserIdAndYearAndEditionType(id, year, EditionType.BOOK);
            Long audiobookCount = userBookRepository.countBooksByUserIdAndYearAndEditionType(id, year, EditionType.AUDIOBOOK);
            Long ebookCount = userBookRepository.countBooksByUserIdAndYearAndEditionType(id, year, EditionType.EBOOK);

            bookStatisticDtos.add(new BookStatisticDto(year, audiobookCount, bookCount, ebookCount));
        }
        return bookStatisticDtos;
    }

    public Map<Bookstore, Long> getStatisticsBookstore(Long id, List<Bookstore> allBookstores) {
        Map<Bookstore, Long> statisticsBookstore = new HashMap<>();
        for (Bookstore bookstore : allBookstores) {
            Long count = userBookRepository.countReadBooksByUserIdAndBookstoreId(id, bookstore.getId());
            if (count > 0)
                statisticsBookstore.put(bookstore, count);
        }
        return statisticsBookstore;
    }

    public List<UserBook> findUserBooksByBookstore(Integer idBookstore) {
        return userBookRepository.findAllByBookstore(idBookstore);
    }
}