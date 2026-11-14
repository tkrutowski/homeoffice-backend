package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.model.BookTextSearchResult;

import java.util.List;

public interface SearchBookUseCase {

    /**
     * Szuka książek po tytule i (opcjonalnie) autorze we wszystkich źródłach.
     * Wynik jest posortowany od najlepszego dopasowania; niedostępne źródło jest pomijane.
     */
    List<BookSearchResult> searchBooks(String title, String author);

    /**
     * Szuka książek na podstawie wolnego tekstu, np. "znajdź mi książkę Rodzinny interes autora Chmielarz".
     * Zwraca też to, co zrozumieliśmy z tekstu. Gdy w tekście nie ma tytułu, lista kandydatów jest pusta.
     *
     * @throws net.focik.homeoffice.library.domain.exception.BookQueryExtractionException gdy tekstu nie udało się zinterpretować
     */
    BookTextSearchResult searchBooksByText(String text);
}
