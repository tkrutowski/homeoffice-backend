package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.library.domain.exception.BookAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.BookNotFoundException;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import net.focik.homeoffice.library.domain.port.secondary.UserBookRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final FileRepository fileRepository;
    private final UserBookRepository userBookRepository;
    private final EmailNotificationPort emailNotificationPort;

    private final UserFacade userFacade;


    public Book addBook(Book book) {
        log.debug("Adding book {}", book);
        if (isBookExist(book))
            throw new BookAlreadyExistException(book);
        book.setCover(fileRepository.downloadAndSaveImage(book.getCover(), book.getTitle(), Module.BOOK));
        return Optional.of(bookRepository.add(book))
                .get().orElse(null);
    }

    private boolean isBookExist(Book book) {
        List<Book> allByTitle = bookRepository.findAllByTitle(book.getTitle());
        log.debug("Found {} books", allByTitle.size());
        if (!allByTitle.isEmpty()) {
            for (Book bookFound : allByTitle) {
                if (book.equals(bookFound)) {
                    return true;
                }
            }
        }
        return false;
    }


    public Book findBook(Integer id) {
        log.debug("Finding book with id {}", id);
        Optional<Book> bookById = bookRepository.findById(id);
        if (bookById.isEmpty()) {
            return null;
        }
        log.debug("Found book {}", bookById);
        return bookById.get();
    }

    public List<Book> findAllBooks() {
        return bookRepository.findAll();
    }


    public Book updateBook(Book book) {
        log.debug("Updating book {}", book);
        if (!book.getCover().contains("focik-home")) {
            book.setCover(fileRepository.downloadAndSaveImage(book.getCover(), book.getTitle(), Module.BOOK));
        }

        Optional<Book> updatedBook = bookRepository.update(book);
        if (updatedBook.isEmpty()) {
            throw new BookNotFoundException(book.getTitle());
        }
        log.debug("Updated book {}", updatedBook);
        return updatedBook.get();
    }

    public void deleteBook(Integer id) {
        bookRepository.delete(id);
    }

    public List<Book> findAllBooksInSeries(Series series) {
        return bookRepository.findAllBySeries(series);
    }

    public Page<Book> findBooksPageable(int page, int size, String sortField, String sortDirection) {
        Pageable pageable;

        if (sortField == null || sortField.isEmpty() || "null".equals(sortField)) {
            // Domyślne sortowanie po ID malejąco
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        } else {
            String jpaField = switch (sortField) {
                case "authors" -> "authors.lastName";
                case "series" -> "series.title";
                case "categories" -> "categories.name";
                default -> sortField;
            };

            Sort.Direction direction = Sort.Direction.fromString(sortDirection);
            pageable = PageRequest.of(page, size, Sort.by(direction, jpaField));
        }
        return bookRepository.findAll(pageable);
    }

    public Page<Book> findBooksPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter,
                                                   String title,
                                                   String author,
                                                   String category,
                                                   String series) {
        String jpaField = switch (sortField) {
            case "authors" -> "authors.lastName";
            case "series" -> "series.title";
            case "categories" -> "categories.name";
            default -> sortField.isEmpty() || "null".equals(sortField) ? "id" : sortField;
        };

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, jpaField));

        return bookRepository.findBooksWithFilters(
                globalFilter,
                title,
                author,
                category,
                series,
                pageable
        );
    }

    public Map<Author, Long> getStatistics(List<Author> allAuthors) {
        Map<Author, Long> statistics = new HashMap<>();
        for (Author author : allAuthors) {
            Long count = bookRepository.countBooksByAuthorId(author.getId());
            if (count > 0)
                statistics.put(author, count);
        }
        return statistics;
    }

    public List<Book> findAllBooksByAuthor(Integer authorId) {
        return bookRepository.findAllByAuthor(authorId);
    }


    // ============ EMAIL NOTIFICATION METHODS ============

    /**
     * Send monthly books summary emails to all users.
     * Scheduled to run on the 1st day of each month at 9:00 AM (Europe/Warsaw timezone).
     *
     * This method:
     * - Gets all users from system
     * - Calculates which books each user read in current month
     * - Sends personalized email with summary
     */
    @Scheduled(cron = "0 0 9 1 * *", zone = "Europe/Warsaw")
    public void sendMonthlySummariesToAllUsers() {
        log.info("Starting scheduled monthly books summary email task");

        try {
            // Get all users in the system
            List<AppUser> allUsers = userFacade.getAllUsers();
            log.info("Found {} users to send summaries to", allUsers.size());

            // Send summary for each user
            allUsers.forEach(user -> sendMonthlyBooksSummary(user.getId()));

            log.info("Completed scheduled monthly books summary email task");
        } catch (Exception e) {
            log.error("Failed during monthly books summary email task", e);
        }
    }

    /**
     * Send monthly books summary to a specific user.
     *
     * @param userId the user ID to send the summary to
     */
    public void sendMonthlyBooksSummary(Long userId) {
        log.info("Sending monthly books summary for user: {}", userId);

        try {
            AppUser user = userFacade.findUserById(userId);

            if (user == null) {
                log.warn("User {} not found", userId);
                return;
            }

            // Validation
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("User {} has no email address", userId);
                return;
            }

            // Get PREVIOUS month date range
            // (Scheduler runs on 1st of current month, so we send summary for previous month)
            LocalDate now = LocalDate.now();
            LocalDate previousMonth = now.minusMonths(1);
            LocalDate monthStart = previousMonth.withDayOfMonth(1);
            LocalDate monthEnd = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());

            // Get books read by user in PREVIOUS month
            List<UserBook> monthlyBooks = userBookRepository.findAllByUserAndReadStatusAndYear(
                userId,
                ReadingStatus.READ,
                monthStart,
                monthEnd
            );

            log.debug("Found {} books read by user {} in {}",
                    monthlyBooks.size(), userId, YearMonth.from(previousMonth));

            // Prepare template variables (for previous month)
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("userName", user.getFirstName());
            templateVariables.put("monthYear", monthStart.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("pl", "PL"))
            ));
            templateVariables.put("booksCount", monthlyBooks.size());
            templateVariables.put("books", monthlyBooks);
            templateVariables.put("currentYear", now.getYear());  // Current year for footer

            // Build email request
            EmailRequest request = new EmailRequest(
                    user.getEmail(),
                    "📚 Twoje podsumowanie przeczytanych książek",
                    "monthly-books-summary",
                    templateVariables
            );

            // Send email (asynchronously)
            emailNotificationPort.sendTemplatedEmail(request);

            log.info("Monthly books summary sent to user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to send monthly books summary for user: {}", userId, e);
        }
    }

    /**
     * Send yearly reading statistics to a user.
     * Useful for end-of-year summaries.
     *
     * @param userId the user ID to send statistics to
     * @param year the year to calculate statistics for
     */
    public void sendYearlyReadingStatistics(Long userId, Integer year) {
        log.info("Sending yearly reading statistics for user: {} year: {}", userId, year);

        try {
            AppUser user = userFacade.findUserById(userId);

            if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("Cannot send statistics: user {} not found or has no email", userId);
                return;
            }

            // Get books read in the year
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);

            List<UserBook> yearlyBooks = userBookRepository.findAllByUserAndReadStatusAndYear(
                    userId,
                    ReadingStatus.READ,
                    yearStart,
                    yearEnd
            );

            // Count by month
            Map<String, Integer> booksByMonth = new HashMap<>();
            for (UserBook ub : yearlyBooks) {
                if (ub.getReadTo() != null) {
                    String month = ub.getReadTo().format(DateTimeFormatter.ofPattern("MMMM", new Locale("pl", "PL")));
                    booksByMonth.put(month, booksByMonth.getOrDefault(month, 0) + 1);
                }
            }

            // Prepare template variables
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("userName", user.getFirstName());
            templateVariables.put("year", year);
            templateVariables.put("totalBooksRead", yearlyBooks.size());
            templateVariables.put("booksByMonth", booksByMonth);
            templateVariables.put("books", yearlyBooks);
            templateVariables.put("averageBooksPerMonth",
                    yearlyBooks.size() > 0 ? yearlyBooks.size() / 12 : 0);
            templateVariables.put("currentYear", LocalDate.now().getYear());

            // Build email request
            EmailRequest request = new EmailRequest(
                    user.getEmail(),
                    String.format("📖 Twoje podsumowanie czytania za rok %d", year),
                    "yearly-reading-statistics",
                    templateVariables
            );

            // Send email
            emailNotificationPort.sendTemplatedEmail(request);

            log.info("Yearly reading statistics sent to user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to send yearly reading statistics for user: {}", userId, e);
        }
    }
}