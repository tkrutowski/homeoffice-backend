package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookStatisticDto;
import net.focik.homeoffice.library.api.dto.UserBookApiDto;
import net.focik.homeoffice.library.api.mapper.ApiUserBookMapper;
import net.focik.homeoffice.library.domain.model.EditionType;
import net.focik.homeoffice.library.domain.model.OwnershipStatus;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.primary.DeleteUserBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.FindUserBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveUserBookUseCase;
import net.focik.homeoffice.utils.UserHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/userbook")
public class UserBookController {

    private final FindUserBookUseCase findUserBookUseCase;
    private final SaveUserBookUseCase saveUserBookUseCase;
    private final DeleteUserBookUseCase deleteUserBookUseCase;
    private final ApiUserBookMapper userBookMapper;

    @GetMapping("/check")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<UserBookApiDto>> checkIfUserbook(@RequestParam(name = "id") int id) {
        String userName = UserHelper.getUserName();
        log.info("Request to check user books for book ID: {} by user: {}", id, userName);
        List<UserBook> userBooksForBookId = findUserBookUseCase.findUserBooksForBookId(id, userName);
        log.info("Found {} user books for book ID: {} by user: {}", userBooksForBookId.size(), id, userName);
        return new ResponseEntity<>(userBooksForBookId.stream()
                .map(userBookMapper::toDto)
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<UserBookApiDto> findById(@PathVariable int id) {
        log.info("Request to find user book with ID: {}", id);
        UserBook userBook = findUserBookUseCase.findUserBook(id);
        if (userBook == null) {
            log.warn("User book with ID {} not found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("User book with ID {} found: {}", id, userBook);
        return new ResponseEntity<>(userBookMapper.toDto(userBook), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<UserBookApiDto>> getAllUserBooks() {
        String userName = UserHelper.getUserName();
        log.info("Request to get all books for user: {}", userName);
        List<UserBook> allBooks = findUserBookUseCase.findUserBooksByUser(userName);

        if (allBooks.isEmpty()) {
            log.warn("No books found for user: {}", userName);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        allBooks.sort(Comparator.comparing(UserBook::getReadFrom, Comparator.nullsLast(Comparator.naturalOrder())));
        log.info("Found {} books for user: {}", allBooks.size(), userName);
        return new ResponseEntity<>(allBooks.stream()
                .peek(userBook -> log.debug("Found user book {}", userBook))
                .map(userBookMapper::toDto)
                .peek(dto -> log.debug("Mapped found user book {}", dto))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<UserBookApiDto>> getAllUserBooksByStatusAndYear(@RequestParam(name = "status") ReadingStatus readingStatus,
                                                         @RequestParam(name = "year", required = false) Integer year) {
        String userName = UserHelper.getUserName();
        log.info("Request to get books for user: {} with status: {} and year: {}", userName, readingStatus, year);

        int targetYear = (year == null) ? LocalDate.now().getYear() : year;

        List<UserBook> allBooks;
        if (readingStatus.equals(ReadingStatus.READ)) {
            allBooks = findUserBookUseCase.findBookByUserAndReadStatusAndYear(userName, readingStatus, targetYear);
        } else {
            allBooks = findUserBookUseCase.findBookByUserAndReadStatus(userName, readingStatus);
        }
        allBooks.sort(Comparator.comparing(UserBook::getReadFrom, Comparator.nullsLast(Comparator.naturalOrder())));

        if (allBooks.isEmpty()) {
            log.warn("No books found for user: {} with status: {} and year: {}", userName, readingStatus, targetYear);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.info("Found {} books for user: {} with status: {} and year: {}", allBooks.size(), userName, readingStatus, targetYear);

        return new ResponseEntity<>(allBooks.stream()
                .peek(userBook -> log.debug("Found user book {}", userBook))
                .map(userBookMapper::toDto)
                .peek(dto -> log.debug("Mapped found user book {}", dto))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BookStatisticDto>> getStatistics() {
        String userName = UserHelper.getUserName();
        log.info("Request to get book statistics for user: {}", userName);
        List<BookStatisticDto> allBooks = findUserBookUseCase.getStatistics(userName);

        if (allBooks.isEmpty()) {
            log.warn("No book statistic found for user: {}", userName);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.info("Found {} book statistic for user: {}", allBooks.size(), userName);
        return new ResponseEntity<>(allBooks.stream()
                .peek(s -> log.debug("Statistic - year: {}, audiobook: {}, book: {}, ebook: {}", s.getYear(), s.getAudiobook(), s.getBook(), s.getEbook()))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserBookApiDto> addUserBook(@RequestBody UserBookApiDto userBookDto) {
        String userName = UserHelper.getUserName();
        log.info("Request to add a new book for user: {}", userName);

        UserBook userBook = userBookMapper.toDomain(userBookDto);
        log.debug("Mapped UserBookApiDto to domain object: {}", userBook);

        UserBook saved = saveUserBookUseCase.addUserBook(userBook, userName);
        log.info("User book added successfully: {}", saved);

        UserBookApiDto dto = userBookMapper.toDto(saved);
        log.debug("Mapped saved user book to DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserBookApiDto> editUserBook(@RequestBody UserBookApiDto userBookDto) {
        log.info("Request to edit book for user.");

        UserBook userBookToAdd = userBookMapper.toDomain(userBookDto);
        log.debug("Mapped UserBookApiDto to domain object: {}", userBookToAdd);

        UserBook saved = saveUserBookUseCase.updateUserBook(userBookToAdd);
        log.info("Book updated successfully: {}", saved);

        UserBookApiDto dto = userBookMapper.toDto(saved);
        log.debug("Mapped saved book to DTO: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/reading_status")
    ResponseEntity<List<ReadingStatus>> getReadingStatus() {
        log.info("Request to retrieve reading statuses.");
        List<ReadingStatus> statuses = List.of(ReadingStatus.values());
        log.info("Retrieved {} reading statuses.", statuses.size());
        return new ResponseEntity<>(statuses, OK);
    }

    @GetMapping("/ownership_status")
    ResponseEntity<List<OwnershipStatus>> getOwnershipStatus() {
        log.info("Request to retrieve ownership statuses.");
        List<OwnershipStatus> statuses = List.of(OwnershipStatus.values());
        log.info("Retrieved {} ownership statuses.", statuses.size());
        return new ResponseEntity<>(statuses, OK);
    }

    @GetMapping("/edition_type")
    ResponseEntity<List<EditionType>> getEditionType() {
        log.info("Request to retrieve edition types.");
        List<EditionType> types = List.of(EditionType.values());
        log.info("Retrieved {} edition types.", types.size());
        return new ResponseEntity<>(types, OK);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    public void deleteUserBook(@PathVariable Integer id) {
        log.info("Request to delete user book with id: {}", id);
        deleteUserBookUseCase.deleteUserBook(id);
        log.info("User book with id: {} deleted successfully.", id);
    }
}
