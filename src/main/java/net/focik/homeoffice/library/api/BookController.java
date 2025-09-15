package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.api.dto.BookApiDto;
import net.focik.homeoffice.library.api.mapper.ApiBookMapper;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.port.primary.DeleteBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.FindBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveBookUseCase;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/book")
public class BookController {

    private final SaveBookUseCase saveBookUseCase;
    private final FindBookUseCase findBookUseCase;
    private final DeleteBookUseCase deleteBookUseCase;
    private final ModelMapper mapper;
    private final ApiBookMapper bookMapper;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<BookApiDto> getById(@PathVariable int id) {
        log.info("Request to get book by id: {}", id);
        Book book = findBookUseCase.findBook(id);
        if (book == null) {
            log.warn("No book found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Book found: {}", book);
        return new ResponseEntity<>(bookMapper.toDto(book), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BookApiDto>> getAllBooks() {
        log.info("Request to get all books");
        List<Book> allBooks = findBookUseCase.findAllBooks();
        log.info("Found {} books", allBooks.size());
        return new ResponseEntity<>(allBooks.stream()
                .peek(book -> log.debug("Found book {}", book))
                .map(bookMapper::toDto)
                .peek(dto -> log.debug("Mapped found book {}", dto))
                .toList(), HttpStatus.OK);
    }

    @GetMapping("/url")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<BookApiDto> getBookByUrl(@RequestParam(name = "url") String url) {
        log.info("Request to get book by URL: {}", url);
        Book bookDtoByUrl = findBookUseCase.findBookByUrl(url);
        if (bookDtoByUrl == null) {
            log.warn("No book found with URL: {}", url);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        BookApiDto dto = bookMapper.toDto(bookDtoByUrl);
        log.info("Book found: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    @GetMapping("/page")
    ResponseEntity<Page<BookApiDto>> getBooksPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "") String sortField,
            @RequestParam(name = "direction", defaultValue = "ASC") String sortDirection,
                @RequestParam(name = "globalFilter", required = false) String globalFilter,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "series", required = false) String series
    ) {
       log.info("Request to get books page with page: {}, size: {}, sort: {}, direction: {}, globalFilter: {}, title: {}, author: {}, category: {}, series: {}", page, size, sortField, sortDirection, globalFilter, title, author, category, series);
        Page<Book> booksPage = findBookUseCase.findBooksPageableWithFilters(page, size, sortField, sortDirection, globalFilter, title, author, category, series);

        Page<BookApiDto> dtoPage = booksPage.map(bookMapper::toDto);

        log.debug("Found {} books on page {} of {}",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getTotalPages());

        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/series/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BookApiDto>> getAllBooksInSeries(@PathVariable int id) {
        log.info("Request to get all books in series with id: {}", id);
        List<BookApiDto> existedBooks = findBookUseCase.findAllBooksInSeries(id).stream()
                .peek(book -> log.debug("Found book {}", book))
                .map(bookMapper::toDto)
                .peek(dto -> log.debug("Mapped found book {}", dto))
                .toList();
        log.info("Found {} books in series with id: {}", existedBooks.size(), id);
        return new ResponseEntity<>(existedBooks.stream()
                .sorted(Comparator.comparing(BookApiDto::getBookInSeriesNo))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/series/new/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BookApiDto>> getNewBooksInSeries(@PathVariable int id, @RequestParam(name = "url") String url) {
        log.info("Request to get new books in series with id: {} and URL: {}", id, url);
        List<BookApiDto> newBooks = findBookUseCase.findNewBooksInSeriesByUrl(id, url).stream()
                .peek(book -> log.debug("Found book {}", book))
                .map(bookDto -> mapper.map(bookDto, BookApiDto.class))
                .peek(dto -> log.debug("Mapped found book {}", dto))
                .toList();

        log.info("Found {} new books in series with id: {} using URL: {}", newBooks.size(), id, url);

        return new ResponseEntity<>(newBooks.stream()
                .sorted(Comparator.comparing(BookApiDto::getBookInSeriesNo))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookApiDto> addBook(@RequestBody BookApiDto bookDto) {
        log.info("Request to add a new book received with data: {}", bookDto);

        Book bookToAdd = bookMapper.toDomain(bookDto);
        log.debug("Mapped Book DTO to domain object: {}", bookToAdd);

        Book bookAdded = saveBookUseCase.addBook(bookToAdd);
        log.info("Book added successfully: {}", bookAdded);

        BookApiDto dto = bookMapper.toDto(bookAdded);
        log.debug("Mapped added book to DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookApiDto> editBook(@RequestBody BookApiDto bookDto) {
        log.info("Request to edit a book received with data: {}", bookDto);

        Book bookToUpdate = bookMapper.toDomain(bookDto);
        log.debug("Mapped Book DTO to domain object: {}", bookToUpdate);

        Book updatedBook = saveBookUseCase.updateBook(bookToUpdate);
        log.info("Book updated successfully: {}", updatedBook);

        BookApiDto dto = bookMapper.toDto(updatedBook);
        log.debug("Mapped added book to DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_DELETE_ALL','LIBRARY_DELETE') or hasRole('ROLE_ADMIN')")
    public void deleteBook(@PathVariable Integer id) {
        log.info("Request to delete book with id: {}", id);
        deleteBookUseCase.deleteBook(id);
        log.info("Book with id: {} deleted successfully", id);
    }
}