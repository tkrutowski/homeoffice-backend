package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.BookSearchResult;

import java.util.List;

public interface SearchBookUseCase {

    /**
     * Szuka książek po tytule i (opcjonalnie) autorze we wszystkich źródłach.
     * Wynik jest posortowany od najlepszego dopasowania; niedostępne źródło jest pomijane.
     */
    List<BookSearchResult> searchBooks(String title, String author);
}
