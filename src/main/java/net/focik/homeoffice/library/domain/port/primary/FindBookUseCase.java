package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.Book;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FindBookUseCase {
    Book findBook(Integer idBook);

    List<Book> findAllBooks();

    Page<Book> findBooksPageable(int page, int size, String sortField, String sortDirection);

    List<Book> findAllBooksInSeries(Integer idSeries);

    Book findBookByUrl(String url);

    List<Book> findNewBooksInSeriesByUrl(Integer idSeries, String urlSeries);

    Page<Book> findBooksPageableWithFilters(int page, int size, String sortField, String sortDirection,
                                            String globalFilter,
                                            String title,
                                            String author,
                                            String category,
                                            String series);

    List<Book> findAllBooksByAuthor(int id);
}
