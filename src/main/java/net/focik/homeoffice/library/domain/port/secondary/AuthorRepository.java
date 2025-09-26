package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface AuthorRepository {

    Author add(Author author);

    Optional<Author> findById(Integer id);

    List<Author> findAll();

    void delete(Integer id);

    Optional<Author> update(Author author);

    Optional<Author> findByFirstNameAndLastName(String firstName, String lastName);

    Page<Author> findAuthorsWithFilters(String globalFilter, Pageable pageable);
}
