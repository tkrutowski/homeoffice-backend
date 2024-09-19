package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
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
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/author")
public class AuthorController {
    final private FindAuthorUseCase authorUseCase;
    private final ApiAuthorMapper authorMapper;
    private final SaveAuthorUseCase saveAuthorUseCase;

    //
    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<AuthorDto>> getAllAuthors() {
        List<Author> allAuthors = authorUseCase.getAllAuthors();
        return new ResponseEntity<>(allAuthors.stream().map(authorMapper::toDto).collect(Collectors.toList()), HttpStatus.OK);
    }

    //
    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<AuthorDto> addAuthor(@RequestBody AuthorDto author) {
        Author added = saveAuthorUseCase.add(authorMapper.toDomain(author));
        return new ResponseEntity<>(authorMapper.toDto(added), HttpStatus.CREATED);
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
