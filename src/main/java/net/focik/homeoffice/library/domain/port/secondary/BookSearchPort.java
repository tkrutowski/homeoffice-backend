package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.model.BookSearchResult;

import java.util.List;

/**
 * Źródło, które potrafi wyszukać książki po tytule i autorze (np. serwis z katalogiem książek).
 * Każdy adapter sam decyduje, jak złożyć zapytanie - serwisy różnie obsługują frazę "tytuł autor".
 */
public interface BookSearchPort {

    /** Krótka nazwa źródła zwracana w wynikach, np. {@code lubimyczytac}. */
    String sourceName();

    /**
     * @param title  tytuł (wymagany)
     * @param author autor, może być pusty
     * @return kandydaci w kolejności trafności według źródła; pusta lista, gdy nic nie znaleziono
     * @throws RuntimeException gdy źródło jest niedostępne lub zablokowało zapytanie
     */
    List<BookSearchResult> search(String title, String author);
}
