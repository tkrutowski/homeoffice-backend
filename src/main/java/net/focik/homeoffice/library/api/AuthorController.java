package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.api.dto.AuthorDto;
import net.focik.homeoffice.library.api.mapper.ApiAuthorMapper;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.port.primary.DeleteAuthorUseCase;
import net.focik.homeoffice.library.domain.port.primary.FindAuthorUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveAuthorUseCase;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/author")
public class AuthorController {
    private final FindAuthorUseCase authorUseCase;
    private final ApiAuthorMapper authorMapper;
    private final SaveAuthorUseCase saveAuthorUseCase;
    private final DeleteAuthorUseCase deleteAuthorUseCase;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<AuthorDto>> getAllAuthors() {
        log.info("Request to get all authors.");
        List<Author> allAuthors = authorUseCase.getAllAuthors();
        if (allAuthors.isEmpty()) {
            log.warn("No authors found.");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        log.info("Found {} authors.", allAuthors.size());
        return new ResponseEntity<>(allAuthors.stream()
                .peek(author -> log.debug("Found author {}", author))
                .map(authorMapper::toDto)
                .peek(dto -> log.debug("Mapped found author {}", dto))
                .toList(), HttpStatus.OK);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<AuthorDto>> getAuthorsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "") String sortField,
            @RequestParam(name = "direction", defaultValue = "ASC") String sortDirection,
            @RequestParam(name = "globalFilter", required = false) String globalFilter) {

        Page<Author> authorPage = authorUseCase.findAuthorsAuthorsPageableWithFilters(page, size, sortField, sortDirection, globalFilter);

        Page<AuthorDto> dtoPage = authorPage.map(authorMapper::toDto);
        log.debug("Found {} books on page {} of {}",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getTotalPages());

        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<AuthorDto> getAuthorsByFirstAndLastName(@RequestParam String firstName, @RequestParam String lastName) {
        log.info("Request to search authors by firstName: {} and lastName: {}", firstName, lastName);
        Author author = authorUseCase.findAuthorsByFirstAndLastName(firstName, lastName);

        if (author == null) {
            log.warn("No author found with firstName: {} and lastName: {}", firstName, lastName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found author matching firstName: {} and lastName: {}", firstName, lastName);
        AuthorDto dto = authorMapper.toDto(author);
        log.debug("Mapped domain object to Author DTO: {}", author);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<Map<AuthorDto, Long>> getStatisticsBookstore() {
        log.info("Request to get author statistics");
        Map<Author, Long> statistics = authorUseCase.getStatistics();

        if (statistics.isEmpty()) {
            log.warn("No books found for authors");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.info("Found {} book statistic for authors", statistics.size());

        Map<AuthorDto, Long> result = statistics.entrySet().stream()
                .peek(e -> log.debug("Statistic - author: {}, count: {}", e.getKey(), e.getValue()))
                .collect(Collectors.toMap(
                        entry -> authorMapper.toDto(entry.getKey()),
                        Map.Entry::getValue
                ));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<AuthorDto> addAuthor(@RequestBody AuthorDto author) {
        log.info("Request to add author: {}", author);

        Author authorToAdd = authorMapper.toDomain(author);
        log.debug("Mapped Author DTO to domain object: {}", authorToAdd);

        Author added = saveAuthorUseCase.add(authorToAdd);
        log.info("Added author: {}", added);

        AuthorDto dto = authorMapper.toDto(added);
        log.debug("Mapped domain object to Author DTO: {}", added);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);

    }

    @PutMapping
    public Author editAuthor(@RequestBody Author author) {
        return saveAuthorUseCase.updateAuthor(author);
    }

    //
//    @GetMapping("/{id}")
//    public Author getAuthor(@PathVariable Long id) {
//        return authorService.findAuthor(id);
//    }
//
    @DeleteMapping("/{id}")
    public void delAuthor(@PathVariable Integer id) {
        log.debug("Request to delete author with id: {}", id);
        deleteAuthorUseCase.deleteAuthor(id);
    }

}
