package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.api.dto.AuthorDto;
import net.focik.homeoffice.library.api.mapper.ApiAuthorMapper;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.port.primary.FindAuthorUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveAuthorUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/author")
public class AuthorController {
    private final FindAuthorUseCase authorUseCase;
    private final ApiAuthorMapper authorMapper;
    private final SaveAuthorUseCase saveAuthorUseCase;

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

    
    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<AuthorDto> addAuthor(@RequestBody AuthorDto author) {
        log.info("Request to add author: {}", author);

        Author authorToAdd = authorMapper.toDomain(author);
        log.debug("Mapped Computer DTO to domain object: {}", authorToAdd);

        Author added = saveAuthorUseCase.add(authorToAdd);
        log.info("Added author: {}", added);

        AuthorDto dto = authorMapper.toDto(added);
        log.debug("Mapped domain object to Computer DTO: {}", added);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);

    }
//
//    @PutMapping
//    public Author editAuthor(@RequestBody Author author) {
//        return authorService.editAuthor(author);
//    }
//
//    @GetMapping("/{id}")
//    public Author getAuthor(@PathVariable Long id) {
//        return authorService.findAuthor(id);
//    }
//
//    @DeleteMapping("/{id}")
//    public void delAuthor(@PathVariable Long id) {
//        authorService.deleteAuthor(id);
//    }

}
