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
     * Odpowiada na wolny tekst, np. "znajdź mi książkę Rodzinny interes autora Chmielarz" albo
     * "znajdź następną książkę z cyklu Bezimienny". Zwraca też to, co zrozumieliśmy z tekstu.
     * Dla pytań o pojedynczą książkę wynikiem są kandydaci (pusta lista, gdy w tekście nie ma tytułu),
     * dla pytań o cykl - odpowiedź o cyklu z biblioteki użytkownika.
     *
     * @param userName użytkownik, którego postęp czytania uwzględniamy w pytaniach o cykl
     * @throws net.focik.homeoffice.library.domain.exception.BookQueryExtractionException gdy tekstu nie udało się zinterpretować
     */
    BookTextSearchResult searchBooksByText(String text, String userName);
}
