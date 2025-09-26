package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Series;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface BookRepository {

    Optional<Book> add(Book book);

    Optional<Book> update(Book book);

    void delete(Integer id);

    List<Book> findAll();

    Page<Book> findAll(Pageable pageable);

    Page<Book> findBooksWithFilters(
            String globalFilter,
            String title,
            String author,
            String category,
            String series,
            Pageable pageable
    );

    Optional<Book> findById(Integer id);

    List<Book> findAllByTitle(String title);

    List<Book> findAllBySeries(Series series);

    Optional<Book> findByTitle(String title);

    Long countBooksByAuthorId(Integer authorId);

    List<Book> findAllByAuthor(Integer authorId);
}
