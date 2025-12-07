package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.BookAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.BookNotFoundException;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import net.focik.homeoffice.utils.IFileHelper;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class BookService {

    private final BookRepository bookRepository;
    private final IFileHelper fileHelperS3;


    public Book addBook(Book book) {
        log.debug("Adding book {}", book);
        if (isBookExist(book))
            throw new BookAlreadyExistException(book);
        book.setCover(fileHelperS3.downloadAndSaveImage(book.getCover(), book.getTitle(), Module.BOOK));
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
            book.setCover(fileHelperS3.downloadAndSaveImage(book.getCover(), book.getTitle(), Module.BOOK));
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
}