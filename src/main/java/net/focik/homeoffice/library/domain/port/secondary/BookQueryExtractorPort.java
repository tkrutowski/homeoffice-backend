package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.exception.BookQueryExtractionException;
import net.focik.homeoffice.library.domain.model.BookQuery;

/** Rozumie wolny tekst ("znajdź mi książkę X autora Y") i wyciąga z niego tytuł, autora i podpowiedzi. */
public interface BookQueryExtractorPort {

    /**
     * @param text niepusty tekst użytkownika
     * @return zapytanie; pola, których w tekście nie ma, są {@code null}
     * @throws BookQueryExtractionException gdy tekstu nie udało się zinterpretować
     */
    BookQuery extract(String text);
}
