package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.BookAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.BookNotFoundException;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import net.focik.homeoffice.utils.FileHelper;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class BookService {

    private final BookRepository bookRepository;
    private final FileHelper fileHelper;



    public Book addBook(Book book) {
        log.debug("Adding book {}", book);
        if (isBookExist(book))
            throw new BookAlreadyExistException(book);
        book.setCover(fileHelper.downloadAndSaveImage(book.getCover(), book.getTitle(), Module.BOOK));
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
        if (!book.getCover().contains("focikhome")) {
            book.setCover(fileHelper.downloadAndSaveImage(book.getCover(), book.getTitle(), Module.BOOK));
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
}