package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.Author;

public interface SaveAuthorUseCase {
    Author add(Author book);

    Author updateAuthor(Author author);
}
