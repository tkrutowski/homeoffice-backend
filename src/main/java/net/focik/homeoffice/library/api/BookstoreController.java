package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.api.dto.BookstoreDto;
import net.focik.homeoffice.library.domain.model.Bookstore;
import net.focik.homeoffice.library.domain.port.primary.DeleteBookstoreUseCase;
import net.focik.homeoffice.library.domain.port.primary.FindBookstoreUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveBookstoreUseCase;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/bookstore")
public class BookstoreController {
    private final ModelMapper mapper;
    private final FindBookstoreUseCase findBookstoreUseCase;
    private final SaveBookstoreUseCase saveBookstoreUseCase;
    private final DeleteBookstoreUseCase deleteBookstoreUseCase;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ')or hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<BookstoreDto>> getAllBookstores() {
        log.info("Request to get all bookstores");

        List<Bookstore> allBookstores = findBookstoreUseCase.findAllBookstores();
        if (allBookstores.isEmpty()) {
            log.warn("No bookstores found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found {} bookstores", allBookstores.size());
        return new ResponseEntity<>(allBookstores.stream()
                .peek(bookstore -> log.debug("Found bookstore {}", bookstore))
                .map(bookstore -> mapper.map(bookstore, BookstoreDto.class))
                .peek(dto -> log.debug("Mapped found bookstore {}", dto))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ')or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookstoreDto> getBookstore(@PathVariable Integer id) {
        log.info("Request to get bookstore with id: {}", id);
        Bookstore bookstoreById = findBookstoreUseCase.findBookstoreById(id);
        if (bookstoreById == null) {
            log.warn("No bookstore found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found bookstore: {}", bookstoreById);
        BookstoreDto dto = mapper.map(bookstoreById, BookstoreDto.class);
        log.info("Mapped found bookstore: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE')or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookstoreDto> addBookstore(@RequestBody BookstoreDto bookstoreDto) {
        log.info("Request to add a new bookstore received with data: {}", bookstoreDto);

        Bookstore bookstore = mapper.map(bookstoreDto, Bookstore.class);
        log.debug("Mapped Bookstore DTO to domain object: {}", bookstore);

        Bookstore saved = saveBookstoreUseCase.addBookstore(bookstore);
        log.info("Bookstore added successfully: {}", saved);

        BookstoreDto dto = mapper.map(saved, BookstoreDto.class);
        log.debug("Mapped added bookstore to DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PutMapping("/id")
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE')or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookstoreDto> editBookstore(@RequestBody BookstoreDto bookstoreDto) {
        log.info("Request to edit a bookstore received with data: {}", bookstoreDto);

        Bookstore bookstore = mapper.map(bookstoreDto, Bookstore.class);
        log.debug("Mapped Bookstore DTO to domain object: {}", bookstore);

        Bookstore saved = saveBookstoreUseCase.updateBookstore(bookstore);
        log.info("Bookstore updated successfully: {}", saved);

        BookstoreDto dto = mapper.map(saved, BookstoreDto.class);
        log.debug("Mapped added bookstore to DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_DELETE_ALL','LIBRARY_DELETE')or hasRole('ROLE_ADMIN')")
    public void deleteBookstore(@PathVariable Integer id) {
        log.info("Request to delete bookstore with id: {}", id);
        deleteBookstoreUseCase.deleteBookstore(id);
        log.info("Bookstore deleted successfully with id: {}", id);
    }
}