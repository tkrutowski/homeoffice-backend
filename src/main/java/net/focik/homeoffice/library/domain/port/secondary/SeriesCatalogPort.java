package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.model.BookSearchResult;

import java.util.List;

/**
 * Źródło, które zna pełną listę tomów cyklu (np. strona cyklu w serwisie z katalogiem książek).
 * Adres cyklu zapisany w bibliotece może wskazywać różne serwisy, więc port najpierw mówi, czy go obsługuje.
 */
public interface SeriesCatalogPort {

    /** Czy ten adapter potrafi pobrać tomy z podanego adresu cyklu. */
    boolean supports(String seriesUrl);

    /**
     * @return tomy cyklu w kolejności źródła; {@code bookInSeriesNo} jest {@code null}, gdy źródło nie numeruje tomów
     * @throws RuntimeException gdy źródło jest niedostępne lub zablokowało zapytanie
     */
    List<BookSearchResult> findVolumes(String seriesUrl);
}
