package net.focik.homeoffice.library.domain;

import net.focik.homeoffice.library.domain.exception.SeriesNotFoundException;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.userservice.domain.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LibraryFacade - addBook")
class LibraryFacadeAddBookTest {

    @Mock
    private UserFacade userFacade;
    @Mock
    private BookService bookService;
    @Mock
    private BookScraperService scraperService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private UserBookService userBookService;
    @Mock
    private BookstoreService bookstoreService;
    @Mock
    private AuthorService authorService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AudiobookAvailabilityService audiobookAvailabilityService;

    private LibraryFacade facade;

    @BeforeEach
    void setUp() {
        facade = new LibraryFacade(userFacade, bookService, scraperService, seriesService, userBookService,
                bookstoreService, authorService, categoryService, audiobookAvailabilityService);
        when(categoryService.validCategories(Set.of())).thenReturn(Set.of());
        when(authorService.validAuthors(Set.of())).thenReturn(Set.of());
    }

    private Book bookWith(Series series) {
        return Book.builder().title("Tytul").series(series).authors(Set.of()).categories(Set.of()).build();
    }

    @Test
    @DisplayName("should save a book without series instead of failing with NullPointerException")
    void addBook_ShouldSave_WhenSeriesIsNull() {
        Book book = bookWith(null);
        when(bookService.addBook(book)).thenReturn(book);

        Book result = facade.addBook(book);

        assertThat(result).isSameAs(book);
        assertThat(result.getSeries()).isNull();
        verify(seriesService, never()).validSeries(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("should replace the series with the one stored in the database")
    void addBook_ShouldUseStoredSeries_WhenSeriesGiven() {
        Series given = new Series();
        given.setId(5);
        Series stored = new Series();
        stored.setId(5);
        stored.setTitle("Cykl");
        Book book = bookWith(given);
        when(seriesService.validSeries(given)).thenReturn(stored);
        when(bookService.addBook(book)).thenReturn(book);

        Book result = facade.addBook(book);

        assertThat(result.getSeries()).isSameAs(stored);
    }

    @Test
    @DisplayName("should still reject a series that does not exist")
    void addBook_ShouldThrow_WhenSeriesNotFound() {
        Series given = new Series();
        given.setId(99);
        Book book = bookWith(given);
        when(seriesService.validSeries(given)).thenThrow(new SeriesNotFoundException(99));

        assertThatThrownBy(() -> facade.addBook(book)).isInstanceOf(SeriesNotFoundException.class);
        verify(bookService, never()).addBook(org.mockito.ArgumentMatchers.any());
    }
}
