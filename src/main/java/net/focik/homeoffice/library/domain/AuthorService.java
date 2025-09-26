package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.domain.exception.AuthorAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.AuthorNotFoundException;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.port.secondary.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class AuthorService {

    private final AuthorRepository authorRepository;

    public Author addAuthor(Author author) {
        Optional<Author> optionalAuthor = authorRepository.findByFirstNameAndLastName(author.getFirstName(), author.getLastName());
        if (optionalAuthor.isPresent()) {
            throw new AuthorAlreadyExistException(author);
        }
        return authorRepository.add(author);
    }

    public List<Author> findAllAuthors() {
        return authorRepository.findAll();
    }

    public void deleteAuthor(Integer id) {
        authorRepository.delete(id);
    }

    public Author updateAuthor(Author author) {
        Author existingAuthor = authorRepository.findById(author.getId())
                .orElseThrow(() -> new AuthorNotFoundException(author.getId()));

        Author updatedAuthor = Author.builder()
                .id(existingAuthor.getId())
                .firstName(author.getFirstName())
                .lastName(author.getLastName())
                .build();

        return authorRepository.update(updatedAuthor)
                .orElseThrow(() -> new AuthorNotFoundException(author.getId()));
    }

    public Author findAuthor(Integer id) {
      return authorRepository.findById(id).orElse(null);
    }

    public Author findAuthorsByFirstAndLastName(String firstName, String lastName) {
        return authorRepository.findByFirstNameAndLastName(firstName, lastName).orElse(null);
    }

    public Page<Author> findAuthorsAuthorsPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter) {
        Pageable pageable;

        if (sortField == null || sortField.isEmpty()){
            // Domyślne sortowanie po ID malejąco
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        }else {
            Sort.Direction direction = Sort.Direction.fromString(sortDirection);
            pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        }
        return authorRepository.findAuthorsWithFilters(globalFilter, pageable);
    }
}
