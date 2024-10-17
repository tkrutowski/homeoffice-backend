package net.focik.homeoffice.library.domain;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.BookAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.BookNotFoundException;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
class BookService {

    private final BookRepository bookRepository;
    @Value("${covers.directory}")
    private final String coverCatalogUrl;
    @Value("${covers.url}")
    private final String homeUrl;

    BookService(BookRepository bookRepository, @Value("${covers.directory}") String coverCatalogUrl,  @Value("${covers.url}")String homeUrl) {
        this.bookRepository = bookRepository;
        this.coverCatalogUrl = coverCatalogUrl;
        this.homeUrl = homeUrl;
    }

    public Book addBook(Book book) {
        log.debug("Adding book {}", book);
        if (isBookExist(book))
            throw new BookAlreadyExistException(book);
        book.setCover(downloadAndSaveImage(book.getCover(), book.getTitle()));
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

    public String downloadAndSaveImage(String imageUrl, String title) {
        try {
            log.debug("Downloading image {}", imageUrl);
            URI uri = new URI(imageUrl);
            URL url = uri.toURL();

            // Pobieranie rozszerzenia pliku z URL
            String path = url.getPath();
            String extension = path.substring(path.lastIndexOf("."));

            String fileName = title.trim().replace(" ", "_") + UUID.randomUUID() + extension; // Generowanie unikalnej nazwy pliku
            File outputFile = new File(coverCatalogUrl + "/" + fileName);
            // Pobierz plik z URL i zapisz go na dysku
            FileUtils.copyURLToFile(url, outputFile, 10000, 10000);

            return homeUrl + fileName;
        } catch (IOException e) {
            log.error("Error downloading ans saving image (return null)",e);
            return null;
        } catch (URISyntaxException e) {
            log.error("Error downloading ans saving image",e);
            throw new RuntimeException(e);
        }
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
        Book bookToEdit = findBook(book.getId());
        if (!book.getCover().contains("focikhome")) {
            bookToEdit.setCover(downloadAndSaveImage(book.getCover(), book.getTitle()));
        } else {
            bookToEdit.setCover(book.getCover());
        }
        bookToEdit.setDescription(book.getDescription());
        bookToEdit.setTitle(book.getTitle());

        Optional<Book> updatedBook = bookRepository.update(bookToEdit);
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