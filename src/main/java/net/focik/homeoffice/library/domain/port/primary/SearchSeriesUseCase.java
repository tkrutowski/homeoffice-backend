package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.BookQueryIntent;
import net.focik.homeoffice.library.domain.model.SeriesAnswer;

public interface SearchSeriesUseCase {

    /**
     * Odpowiada na pytanie o cykl z biblioteki użytkownika ("wszystkie tomy", "nieprzeczytane", "następny po
     * ostatnio przeczytanym"). Nie rzuca wyjątku, gdy cyklu nie da się obsłużyć - zwraca {@link SeriesAnswer}
     * z odpowiednim statusem.
     *
     * @param intent     jedna z intencji dotyczących cyklu ({@link BookQueryIntent#isSeries()})
     * @param seriesName nazwa cyklu w mianowniku, jak podał użytkownik
     * @param userName   użytkownik, którego postęp czytania uwzględniamy
     */
    SeriesAnswer answer(BookQueryIntent intent, String seriesName, String userName);
}
