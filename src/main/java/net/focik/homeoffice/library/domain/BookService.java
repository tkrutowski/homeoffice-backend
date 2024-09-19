package net.focik.homeoffice.library.domain;

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

@Service
class BookService {

    private final BookRepository bookRepository;
    @Value("${covers.directory}")
    private final String coverCatalogUrl;

    BookService(BookRepository bookRepository, @Value("${covers.directory}") String coverCatalogUrl) {
        this.bookRepository = bookRepository;
        this.coverCatalogUrl = coverCatalogUrl;
    }

    public Book addBook(Book book) {
        if (isBookExist(book))
            throw new BookAlreadyExistException(book);
        book.setCover(downloadAndSaveImage(book.getCover(), book.getTitle()));
        return Optional.of(bookRepository.add(book))
                .get().orElse(null);
    }

    private boolean isBookExist(Book book) {
        List<Book> allByTitle = bookRepository.findAllByTitle(book.getTitle());
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
            URI uri = new URI(imageUrl);
            URL url = uri.toURL();

            // Pobieranie rozszerzenia pliku z URL
            String path = url.getPath();
            String extension = path.substring(path.lastIndexOf("."));

            String fileName = "cover_" + title.trim().replace(" ", "_") + extension; // Generowanie unikalnej nazwy pliku
            File outputFile = new File(coverCatalogUrl + "/" + fileName);
            // Pobierz plik z URL i zapisz go na dysku
            FileUtils.copyURLToFile(url, outputFile, 10000, 10000);

            return "https://focikhome.synology.me/covers/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Book findBook(Integer id) {
        Optional<Book> bookById = bookRepository.findById(id);
        if (bookById.isEmpty()) {
            throw new BookNotFoundException(id);
        }
        return bookById.get();
    }

    public List<Book> findAllBooks() {
        return bookRepository.findAll();
    }


    public Book updateBook(Book book) {
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
        return updatedBook.get();
    }

    public void deleteBook(Integer id) {
        bookRepository.delete(id);
    }

    public List<Book> findAllBooksInSeries(Series series) {
        return bookRepository.findAllBySeries(series);
    }
}