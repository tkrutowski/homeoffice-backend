package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.Author;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface FindAuthorUseCase {
    Author getAuthor(Integer idAuthor);

    List<Author> getAllAuthors();

    Author findAuthorsByFirstAndLastName(String firstName, String lastName);

    Map<Author, Long> getStatistics();

    Page<Author> findAuthorsAuthorsPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter);
}
