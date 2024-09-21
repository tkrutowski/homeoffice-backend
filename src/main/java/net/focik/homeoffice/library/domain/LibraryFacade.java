package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.*;
import net.focik.homeoffice.library.domain.port.primary.*;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
        log.debug("Found new books in series with titles {}", existingTitles);
        List<Book> booksInSeries = scraperService.findBooksInSeries(urlSeries, existingTitles);
        log.debug("Found new books in series with titles {}", booksInSeries.stream().map(Book::getTitle).collect(Collectors.joining(";")));

        series.setCheckDate(LocalDateTime.now());
        series.setHasNewBooks(!booksInSeries.isEmpty());
        seriesService.saveSeries(series);
        return booksInSeries;
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
    public List<Bookstore> findAllBookstores() {
        return bookstoreService.findAllBookstores();
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
        return userBookService.updateUserBook(userBook);
    }

    @Override
    public boolean deleteUserBook(Integer idUseBook) {
        userBookService.deleteUserBook(idUseBook);
        return true;
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
    public Category getById(Integer idCategory) {
        return null;
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

    @Scheduled(cron = "0 0 8 * * FRI") // Uruchamia metodę co piątek o 8 rano
    public void findNewBooksInSeriesScheduler(){
        log.info("Scheduled book finder started on {}",LocalDateTime.now());
        List<Series> seriesToProcess = seriesService.getAllSeries().stream()
                .filter(series -> !series.getHasNewBooks())
                .toList();

        log.debug("Series to process: {}", seriesToProcess.size());
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            for (int i = 0; i < seriesToProcess.size(); i++){
                Optional<String[]> urlsOptional = Optional.ofNullable( seriesToProcess.get(i).getUrl())
                        .map(s -> s.split(";;"));
                if (urlsOptional.isPresent()){
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