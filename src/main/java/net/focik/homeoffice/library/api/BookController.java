package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.library.api.dto.BookApiDto;
import net.focik.homeoffice.library.api.mapper.ApiBookMapper;
import net.focik.homeoffice.library.domain.LibraryFacade;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.port.primary.DeleteBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.FindBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveBookUseCase;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/book")
public class BookController {

    private final SaveBookUseCase saveBookUseCase;
    private final FindBookUseCase findBookUseCase;
    private final DeleteBookUseCase deleteBookUseCase;
    private final ModelMapper mapper;
    private final ApiBookMapper bookMapper;

    private final  LibraryFacade testFacade;


    @GetMapping("/test")
    public void test()    {
        testFacade.findNewBooksInSeriesScheduler();
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<BookApiDto> getById(@PathVariable int id) {
        Book book = findBookUseCase.findBook(id);
        return new ResponseEntity<>(bookMapper.toDto(book), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BookApiDto>> getAllBooks() {
        List<Book> allBooks = findBookUseCase.findAllBooks();
        return new ResponseEntity<>(allBooks.stream().map(bookMapper::toDto).collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/url")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<BookApiDto> getBookByUrl(@RequestParam(name = "url") String url) {
        Book bookDtoByUrl = findBookUseCase.findBookByUrl(url);
        BookApiDto dto = bookMapper.toDto(bookDtoByUrl);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/series/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BookApiDto>> getAllBooksInSeries(@PathVariable int id) {
        List<BookApiDto> existedBooks = findBookUseCase.findAllBooksInSeries(id).stream()
                .map(bookMapper::toDto)
                .toList();
        return new ResponseEntity<>(existedBooks.stream()
                .sorted(Comparator.comparing(BookApiDto::getBookInSeriesNo))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/series/new/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<BookApiDto>> getNewBooksInSeries(@PathVariable int id, @RequestParam( name = "url") String url) {
        List<BookApiDto> newBooks = findBookUseCase.findNewBooksInSeriesByUrl(id, url).stream()
                .map(bookDto -> mapper.map(bookDto, BookApiDto.class))
                .toList();

        return new ResponseEntity<>(newBooks.stream()
                .sorted(Comparator.comparing(BookApiDto::getBookInSeriesNo))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookApiDto> addBook(@RequestBody BookApiDto bookDto) {
        Book bookToAdd = bookMapper.toDomain(bookDto);
        Book bookAdded = saveBookUseCase.addBook(bookToAdd);
        return new ResponseEntity<>(bookMapper.toDto(bookAdded), HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookApiDto> editBook(@RequestBody BookApiDto bookDto) {
        Book bookToUpdate = bookMapper.toDomain(bookDto);
        Book updateBook = saveBookUseCase.updateBook(bookToUpdate);
        return new ResponseEntity<>(bookMapper.toDto(updateBook), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_DELETE_ALL','LIBRARY_DELETE') or hasRole('ROLE_ADMIN')")
    public void deleteBook(@PathVariable Integer id) {
        deleteBookUseCase.deleteBook(id);
    }
}