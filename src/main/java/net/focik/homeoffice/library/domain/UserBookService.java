package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.UserBookNotFoundException;
import net.focik.homeoffice.library.domain.model.BookStatisticDto;
import net.focik.homeoffice.library.domain.model.EditionType;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.secondary.UserBookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
class UserBookService {

    private final UserBookRepository userBookRepository;

    public List<UserBook> findUserBooksForBookId(Integer idBook, Integer idUser) {
        List<UserBook> allByBook = userBookRepository.findAllByIdBook(idBook);
        return allByBook.stream()
                .filter(userBook -> idUser.equals(userBook.getUser().getId().intValue()))
                .collect(Collectors.toList());
    }

    public UserBook addUserBook(UserBook userBook) {
        return userBookRepository.add(userBook);
    }

    public UserBook updateUserBook(UserBook userBook) {
        log.debug("Trying to update userBook with id: {}", userBook.getId());
        Optional<UserBook> userBookById = userBookRepository.findById(userBook.getId());
        if (userBookById.isEmpty()) {
            log.warn("UserBook with id {} not found", userBook.getId());
            throw new UserBookNotFoundException(userBook.getId());
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
}