package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.*;
import net.focik.homeoffice.library.domain.port.primary.*;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryFacade implements FindBookUseCase, SaveBookUseCase, DeleteBookUseCase, SaveUserBookUseCase,
        FindSeriesUseCase, FindUserBookUseCase, FindBookstoreUseCase, SaveBookstoreUseCase, DeleteBookstoreUseCase,
        DeleteUserBookUseCase, FindAuthorUseCase, FindCategoryUseCase, SaveAuthorUseCase, SaveCategoryUseCase, SaveSeriesUseCase {

    private final UserFacade userFacade;
    private final BookService bookService;
    private final BookScraperService scraperService;
    private final SeriesService seriesService;
    private final UserBookService userBookService;
    private final BookstoreService bookstoreService;
    private final AuthorService authorService;
    private final CategoryService categoryService;

    @Override
    public Book findBook(Integer idBook) {
        return bookService.findBook(idBook);
    }

    @Override
    public List<Book> findAllBooks() {
        return bookService.findAllBooks();
    }

    @Override
    public Page<Book> findBooksPageable(int page, int size, String sortField, String sortDirection) {
        return bookService.findBooksPageable(page, size, sortField, sortDirection);
    }

    @Override
    public List<Book> findAllBooksInSeries(Integer idSeries) {
        Series series = seriesService.findSeries(idSeries);
        return bookService.findAllBooksInSeries(series);
    }

    @Override
    public Book findBookByUrl(String url) {
        return scraperService.findBookByUrl(url);
    }

    @Override
    public List<Book> findNewBooksInSeriesByUrl(Integer idSeries, String urlSeries) {
        Series series = seriesService.findSeries(idSeries);
        log.debug("Trying to find new books in series {} with URL: {}", series.getTitle(), urlSeries);
        String existingTitles = bookService.findAllBooksInSeries(series).stream()
                .map(Book::getTitle)
                .collect(Collectors.joining(";"));
        log.debug("Found existing books in series with titles {}", existingTitles);
        List<Book> booksInSeries = scraperService.findBooksInSeries(urlSeries, existingTitles);
        log.debug("Found new books in series with titles {}", booksInSeries.stream().map(Book::getTitle).collect(Collectors.joining(";")));

        series.setCheckDate(LocalDateTime.now());
        series.setHasNewBooks(!booksInSeries.isEmpty());
        seriesService.saveSeries(series);
        return booksInSeries;
    }

    @Override
    public Page<Book> findBooksPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String title, String author, String category, String series) {
        return bookService.findBooksPageableWithFilters(page, size, sortField, sortDirection, globalFilter, title, author, category, series);
    }

    @Override
    public Book addBook(Book book) {
        book.setCategories(categoryService.validCategories(book.getCategories()));
        book.setSeries(seriesService.validSeries(book.getSeries()));
        return bookService.addBook(book);
    }

    @Override
    public void deleteBook(Integer idBook) {
        bookService.deleteBook(idBook);
    }

    @Override
    public Book updateBook(Book book) {
        return bookService.updateBook(book);
    }

    @Override
    public Series findSeries(Integer idSeries) {
        return seriesService.findSeries(idSeries);
    }

    @Override
    public Series findSeriesByTitle(String name) {
        return seriesService.findSeriesByTitle(name);
    }

    @Override
    public List<Series> getAllSeries() {
        return seriesService.getAllSeries();
    }

    @Override
    public List<UserBook> findUserBooksForBookId(Integer idBook, String userName) {
        AppUser user = userFacade.findUserByUsername(userName);
        return userBookService.findUserBooksForBookId(idBook, Math.toIntExact(user.getId()));
    }

    @Override
    public List<UserBook> findUserBooksByUser(String userName) {
        AppUser user = userFacade.findUserByUsername(userName);
        return userBookService.findUserBookByUser(user.getId());
    }

    @Override
    public UserBook findUserBook(Integer idUserBook) {
        return userBookService.findUserBook(idUserBook);
    }

    @Override
    public List<UserBook> findBookByUserAndReadStatus(String userName, ReadingStatus readingStatus) {
        AppUser user = userFacade.findUserByUsername(userName);
        return userBookService.findBookByUserAndReadStatus(user.getId(), readingStatus);
    }

    @Override
    public List<UserBook> findBookByUserAndReadStatusAndYear(String userName, ReadingStatus readingStatus, int year) {
        AppUser user = userFacade.findUserByUsername(userName);
        return userBookService.findBookByUserAndReadStatusAndYear(user.getId(), readingStatus, year);
    }

    @Override
    public List<UserBook> findUserBooksByQuery(String userName, String query) {
        AppUser user = userFacade.findUserByUsername(userName);
        return userBookService.findUserBooksByQuery(user.getId(), query);
    }

    @Override
    public List<Bookstore> findAllBookstores() {
        return bookstoreService.findAllBookstores();
    }

    @Override
    public List<BookStatisticDto> getStatistics(String userName) {
        AppUser user = userFacade.findUserByUsername(userName);
        return userBookService.getStatistics(user.getId());
    }

    @Override
    public Map<Bookstore, Long> getStatisticsBookstore(String userName) {
        AppUser user = userFacade.findUserByUsername(userName);
        List<Bookstore> allBookstores = bookstoreService.findAllBookstores();
        return userBookService.getStatisticsBookstore(user.getId(), allBookstores);
    }

    @Override
    public Bookstore findBookstoreById(Integer id) {
        return bookstoreService.findBookstore(id);
    }

    @Override
    public Bookstore addBookstore(Bookstore bookstore) {
        return bookstoreService.addBookstore(bookstore);
    }

    @Override
    public Bookstore updateBookstore(Bookstore bookstore) {
        return bookstoreService.updateBookstore(bookstore);
    }

    @Override
    public void deleteBookstore(Integer idBook) {
        bookstoreService.deleteBookstore(idBook);
    }

    @Override
    public UserBook addUserBook(UserBook userBook, String userName) {
        AppUser user = userFacade.findUserByUsername(userName);
        userBook.setUser(user);
        return userBookService.addUserBook(userBook);
    }

    @Override
    public UserBook updateUserBook(UserBook userBook) {
        UserBook updatedUserBook = userBookService.updateUserBook(userBook);
        if (updatedUserBook.getReadingStatus().equals(ReadingStatus.READ) && Objects.nonNull(updatedUserBook.getBook().getSeries())) {
            seriesService.updateSeries(updatedUserBook.getBook().getSeries(), false);
        }
        return updatedUserBook;
    }

    @Override
    public void deleteUserBook(Integer idUseBook) {
        userBookService.deleteUserBook(idUseBook);
    }

    @Override
    public Author getAuthor(Integer idAuthor) {
        return null;
    }

    @Override
    public List<Author> getAllAuthors() {
        return authorService.findAllAuthors();
    }

    @Override
    public Author findAuthorsByFirstAndLastName(String firstName, String lastName) {
        return authorService.findAuthorsByFirstAndLastName(firstName, lastName);
    }

    @Override
    public Category getById(Integer idCategory) {
        return null;
    }

    @Override
    public Category getByName(String name) {
        return categoryService.findCategoryByName(name);
    }

    @Override
    public List<Category> getFromString(String categories) {
        return categoryService.getFromString(categories);
    }

    @Override
    public List<Category> getAll() {
        return categoryService.findAllCategories();
    }

    @Override
    public Author add(Author author) {
        return authorService.addAuthor(author);
    }

    @Override
    public Category add(Category category) {
        return categoryService.addCategory(category);
    }

    @Scheduled(cron = "${scheduler.cron}") // Uruchamia metodę co piątek o 8 rano
    public void findNewBooksInSeriesScheduler() {
        log.info("Scheduled book finder started on {}", LocalDateTime.now());
        List<Series> seriesToProcess = seriesService.getAllSeries().stream()
                .filter(series -> !series.getHasNewBooks())
                .toList();

        log.info("Series to process: {}", seriesToProcess.size());
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            for (int i = 0; i < seriesToProcess.size(); i++) {
                Optional<String[]> urlsOptional = Optional.ofNullable(seriesToProcess.get(i).getUrl())
                        .map(s -> s.split(";;"));
                if (urlsOptional.isPresent()) {
                    int finalI = i;
                    scheduler.schedule(() -> scheduleUrlsProcessing(seriesToProcess.get(finalI).getId(), List.of(urlsOptional.get())), finalI * 3L, TimeUnit.MINUTES);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void scheduleUrlsProcessing(Integer seriesId, List<String> urls) {
        try {
            for (final String url : urls) {
                log.info("SCHEDULE Processing url: {}", url);
                findNewBooksInSeriesByUrl(seriesId, url);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public Series updateSeries(Series series) {
        return seriesService.updateSeries(series);
    }
}