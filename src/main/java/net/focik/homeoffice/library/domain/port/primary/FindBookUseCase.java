package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.Book;

import java.util.Collection;
import java.util.List;

public interface FindBookUseCase {
    Book findBook(Integer idBook);

    List<Book> findAllBooks();

    List<Book> findAllBooksInSeries(Integer idSeries);

    Book findBookByUrl(String url);

    List<Book> findNewBooksInSeriesByUrl(Integer idSeries, String urlSeries);

}
