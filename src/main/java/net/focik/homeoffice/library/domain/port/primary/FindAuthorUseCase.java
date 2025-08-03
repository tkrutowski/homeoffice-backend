package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.Author;

import java.util.List;

public interface FindAuthorUseCase {
    Author getAuthor(Integer idAuthor);

    List<Author> getAllAuthors();

    Author findAuthorsByFirstAndLastName(String firstName, String lastName);
}
