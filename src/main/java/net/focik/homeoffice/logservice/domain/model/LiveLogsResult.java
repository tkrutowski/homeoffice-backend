package net.focik.homeoffice.logservice.domain.model;

import java.util.List;

/**
 * Odpowiedz na odpytanie o logi "na zywo" biezacej instancji.
 *
 * @param instance nazwa biezacej instancji aplikacji
 * @param entries  wpisy w kolejnosci rosnacej
 * @param cursor   numer ostatniego przejrzanego wpisu - nalezy go przekazac jako {@code after} w nastepnym zapytaniu
 *                 (przesuwa sie takze poza wpisy odfiltrowane po poziomie)
 * @param gap      true, gdy czesc wpisow zostala utracona: kursor byl starszy niz zawartosc bufora albo aplikacja
 *                 zostala zrestartowana (kursor z poprzedniego procesu); klient powinien oznaczyc luke w widoku
 * @param hasMore  true, gdy jest wiecej nowych wpisow niz {@code limit} - klient moze odpytac ponownie od razu
 */
public record LiveLogsResult(String instance, List<LogEntry> entries, long cursor, boolean gap, boolean hasMore) {
}
