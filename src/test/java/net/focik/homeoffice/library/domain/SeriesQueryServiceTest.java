package net.focik.homeoffice.library.domain;

import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.BookQueryIntent;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.model.SeriesAnswer;
import net.focik.homeoffice.library.domain.model.SeriesAnswerStatus;
import net.focik.homeoffice.library.domain.model.SeriesVolume;
import net.focik.homeoffice.library.domain.model.SeriesVolumeStatus;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.primary.FindUserBookUseCase;
import net.focik.homeoffice.library.domain.port.secondary.SeriesCatalogPort;
import net.focik.homeoffice.library.domain.port.secondary.SeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeriesQueryServiceTest {

    private static final String USER = "tomek";
    private static final String LC_URL = "https://lubimyczytac.pl/cykl/32783/bezimienny";

    private SeriesRepository seriesRepository;
    private FindUserBookUseCase findUserBookUseCase;
    private FakeCatalog catalog;
    private SeriesQueryService service;

    /** Źródło obsługujące tylko lubimyczytac.pl; zwraca z góry zadane tomy albo rzuca wyjątek. */
    private static class FakeCatalog implements SeriesCatalogPort {
        List<BookSearchResult> volumes = List.of();
        RuntimeException failure;
        final List<String> requestedUrls = new ArrayList<>();

        public boolean supports(String seriesUrl) {
            return seriesUrl.contains("lubimyczytac.pl");
        }

        public List<BookSearchResult> findVolumes(String seriesUrl) {
            requestedUrls.add(seriesUrl);
            if (failure != null) {
                throw failure;
            }
            return volumes;
        }
    }

    @BeforeEach
    void setUp() {
        seriesRepository = mock(SeriesRepository.class);
        findUserBookUseCase = mock(FindUserBookUseCase.class);
        catalog = new FakeCatalog();
        service = new SeriesQueryService(seriesRepository, findUserBookUseCase, List.of(catalog));
    }

    // --- pomocnicze ---

    private static Series series(int id, String title, String url) {
        return new Series(id, title, null, url, null, null);
    }

    private static BookSearchResult vol(String number, String title) {
        return new BookSearchResult(title, "Wojciech Chmielarz", null, "https://lubimyczytac.pl/ksiazka/" + title.hashCode(),
                "lubimyczytac", "Bezimienny", number);
    }

    private static UserBook owned(int bookId, String title, String number, ReadingStatus status) {
        Book book = Book.builder().id(bookId).title(title).bookInSeriesNo(number).build();
        return UserBook.builder().book(book).readingStatus(status).build();
    }

    private void givenSeries(Series... all) {
        when(seriesRepository.findAll()).thenReturn(List.of(all));
    }

    private void givenCatalog(BookSearchResult... volumes) {
        catalog.volumes = List.of(volumes);
    }

    private void givenUserBooks(int seriesId, UserBook... books) {
        when(findUserBookUseCase.findUserBooksInSeries(USER, seriesId)).thenReturn(List.of(books));
    }

    private SeriesAnswer ask(BookQueryIntent intent, String seriesName) {
        return service.answer(intent, seriesName, USER);
    }

    private static List<String> titles(List<SeriesVolume> volumes) {
        return volumes.stream().map(SeriesVolume::title).toList();
    }

    private void givenBezimienny() {
        givenSeries(series(1, "Bezimienny", "https://upolujebooka.pl/cykl,1,bezimienny.html;;" + LC_URL));
        givenCatalog(vol("1", "Prosta sprawa"), vol("2", "Dług honorowy"), vol("3", "Zwykła przyzwoitość"),
                vol("4", "Rodzinny interes"), vol("5", "Wyższa konieczność"));
    }

    // --- SERIES_ALL ---

    @Test
    void allVolumesAreOrderedByNumberWithStatusFromLibrary() {
        givenBezimienny();
        givenUserBooks(1,
                owned(11, "Prosta sprawa", "1", ReadingStatus.READ),
                owned(12, "Dług honorowy", "2", ReadingStatus.READ_NOW),
                owned(13, "Zwykła przyzwoitość", "3", ReadingStatus.NOT_READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "Bezimienny");

        assertEquals(SeriesAnswerStatus.OK, answer.status());
        assertEquals("Bezimienny", answer.seriesTitle());
        assertEquals(LC_URL, answer.seriesUrl()); // z listy ";;" wybrany został obsługiwany adres
        assertEquals(List.of("Prosta sprawa", "Dług honorowy", "Zwykła przyzwoitość", "Rodzinny interes", "Wyższa konieczność"),
                titles(answer.volumes()));
        assertEquals(List.of(SeriesVolumeStatus.READ, SeriesVolumeStatus.READING, SeriesVolumeStatus.ON_SHELF,
                        SeriesVolumeStatus.NOT_IN_LIBRARY, SeriesVolumeStatus.NOT_IN_LIBRARY),
                answer.volumes().stream().map(SeriesVolume::status).toList());
        assertEquals(11, answer.volumes().get(0).bookId());
        assertNull(answer.volumes().get(3).bookId());
        assertNull(answer.next());
    }

    @Test
    void bookCountsAsReadWhenAnyEditionIsRead() {
        givenBezimienny();
        givenUserBooks(1,
                owned(11, "Prosta sprawa", "1", ReadingStatus.NOT_READ),
                owned(11, "Prosta sprawa", "1", ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "Bezimienny");

        assertEquals(SeriesVolumeStatus.READ, answer.volumes().get(0).status());
    }

    // --- SERIES_UNREAD ---

    @Test
    void unreadReturnsEverythingExceptReadVolumes() {
        givenBezimienny();
        givenUserBooks(1,
                owned(11, "Prosta sprawa", "1", ReadingStatus.READ),
                owned(12, "Dług honorowy", "2", ReadingStatus.READ_NOW));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_UNREAD, "Bezimienny");

        assertEquals(List.of("Dług honorowy", "Zwykła przyzwoitość", "Rodzinny interes", "Wyższa konieczność"),
                titles(answer.volumes()));
    }

    @Test
    void unreadIsEmptyButOkWhenEverythingIsRead() {
        givenBezimienny();
        givenUserBooks(1,
                owned(1, "Prosta sprawa", "1", ReadingStatus.READ),
                owned(2, "Dług honorowy", "2", ReadingStatus.READ),
                owned(3, "Zwykła przyzwoitość", "3", ReadingStatus.READ),
                owned(4, "Rodzinny interes", "4", ReadingStatus.READ),
                owned(5, "Wyższa konieczność", "5", ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_UNREAD, "Bezimienny");

        assertEquals(SeriesAnswerStatus.OK, answer.status());
        assertTrue(answer.volumes().isEmpty());
    }

    // --- SERIES_NEXT ---

    @Test
    void nextIsFirstVolumeAfterLastRead() {
        givenBezimienny();
        givenUserBooks(1,
                owned(11, "Prosta sprawa", "1", ReadingStatus.READ),
                owned(12, "Dług honorowy", "2", ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_NEXT, "Bezimienny");

        assertEquals(SeriesAnswerStatus.OK, answer.status());
        assertEquals("Dług honorowy", answer.lastRead().title());
        assertEquals("Zwykła przyzwoitość", answer.next().title());
        assertTrue(answer.volumes().isEmpty());
    }

    @Test
    void nextUsesHighestReadNumberEvenWhenReadOutOfOrder() {
        givenBezimienny();
        givenUserBooks(1,
                owned(11, "Prosta sprawa", "1", ReadingStatus.READ),
                owned(14, "Rodzinny interes", "4", ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_NEXT, "Bezimienny");

        assertEquals("Rodzinny interes", answer.lastRead().title());
        assertEquals("Wyższa konieczność", answer.next().title());
    }

    @Test
    void nextIsFirstNumberedVolumeWhenNothingReadSkippingVolumeZero() {
        givenSeries(series(1, "Jack Reacher", LC_URL));
        givenCatalog(vol("0", "Zasady"), vol("1", "Poziom śmierci"), vol("2", "Zła wiadomość"));
        givenUserBooks(1);

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_NEXT, "Jack Reacher");

        assertNull(answer.lastRead());
        assertEquals("Poziom śmierci", answer.next().title());
    }

    @Test
    void nextReportsNoNextVolumeWhenLastKnownVolumeIsRead() {
        givenBezimienny();
        givenUserBooks(1, owned(15, "Wyższa konieczność", "5", ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_NEXT, "Bezimienny");

        assertEquals(SeriesAnswerStatus.NO_NEXT_VOLUME, answer.status());
        assertEquals("Wyższa konieczność", answer.lastRead().title());
        assertNull(answer.next());
    }

    @Test
    void nextFollowsFractionalNumbersInOrder() {
        givenSeries(series(1, "Jack Reacher", LC_URL));
        givenCatalog(vol("15", "Pietnasty"), vol("16", "Szesnasty"), vol("15.5", "Opowiadanie"));
        givenUserBooks(1, owned(1, "Pietnasty", "15", ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_NEXT, "Jack Reacher");

        assertEquals("Opowiadanie", answer.next().title());
    }

    @Test
    void nextWithUserBookMissingFromSourceStillCountsAsLastRead() {
        givenBezimienny();
        // tom 7 przeczytany, ale źródło go nie zwróciło (np. niepełna lista) - nie wolno wrócić do tomu 2
        givenUserBooks(1,
                owned(11, "Prosta sprawa", "1", ReadingStatus.READ),
                owned(17, "Tom spoza źródła", "7", ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_NEXT, "Bezimienny");

        assertEquals(SeriesAnswerStatus.NO_NEXT_VOLUME, answer.status());
        assertEquals("Tom spoza źródła", answer.lastRead().title());
        assertNull(answer.lastRead().url());
    }

    @Test
    void nextIsNotOrderedWhenSourceHasNoVolumeNumbers() {
        givenSeries(series(1, "Astrofizyka w kieszeni", "https://lubimyczytac.pl/seria/28336/astrofizyka-w-kieszeni"));
        givenCatalog(vol(null, "Czarne dziury"), vol(null, "Wszechświat"));
        givenUserBooks(1);

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_NEXT, "Astrofizyka w kieszeni");
        SeriesAnswer all = ask(BookQueryIntent.SERIES_ALL, "Astrofizyka w kieszeni");

        assertEquals(SeriesAnswerStatus.NOT_ORDERED, answer.status());
        assertEquals(List.of("Czarne dziury", "Wszechświat"), titles(all.volumes())); // lista nadal działa
    }

    // --- łączenie z biblioteką ---

    @Test
    void bookWithDifferentTitleMatchesByNumberOnlyWhenNumberIsUnique() {
        givenSeries(series(1, "Jack Reacher", LC_URL));
        givenCatalog(vol("1", "Poziom śmierci"), vol("11", "Jedenasty"), vol("11", "Eleventh"));
        givenUserBooks(1,
                owned(1, "Killing Floor", "1", ReadingStatus.READ),   // numer 1 jest unikalny -> dopasowanie po numerze
                owned(2, "Inny tytuł", "11", ReadingStatus.READ));    // numer 11 występuje dwa razy -> nie zgadujemy

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "Jack Reacher");

        List<SeriesVolume> volumes = answer.volumes();
        assertEquals(SeriesVolumeStatus.READ, volumes.get(0).status());
        assertEquals(1, volumes.get(0).bookId());
        assertEquals(SeriesVolumeStatus.NOT_IN_LIBRARY, volumes.get(1).status());
        assertEquals(SeriesVolumeStatus.NOT_IN_LIBRARY, volumes.get(2).status());
        // niedopasowana książka użytkownika nie ginie: jest dopisana z numerem 11
        SeriesVolume extra = volumes.get(3);
        assertEquals("Inny tytuł", extra.title());
        assertEquals(2, extra.bookId());
        assertNull(extra.url());
    }

    @Test
    void titlesAreMatchedIgnoringCaseDiacriticsAndPunctuation() {
        givenSeries(series(1, "Jack Reacher", LC_URL));
        givenCatalog(vol("1", "Jack Reacher. Zasady"));
        givenUserBooks(1, owned(1, "JACK REACHER ZASADY", null, ReadingStatus.READ));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "Jack Reacher");

        assertEquals(SeriesVolumeStatus.READ, answer.volumes().get(0).status());
        assertEquals(1, answer.volumes().size());
    }

    // --- rozpoznawanie cyklu ---

    @Test
    void seriesNameInOtherGrammaticalFormIsMatched() {
        givenBezimienny();
        givenUserBooks(1);

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "Bezimiennym");

        assertEquals(SeriesAnswerStatus.OK, answer.status());
        assertEquals("Bezimienny", answer.seriesTitle());
    }

    @Test
    void exactTitleBeatsLongerTitleContainingAllWords() {
        givenSeries(
                series(1, "The Frontiers Saga Part 2: Rogue Castes", LC_URL),
                series(2, "The Frontiers Saga", "https://lubimyczytac.pl/cykl/27051/the-frontiers-saga"));
        givenCatalog(vol("1", "Tom"));
        givenUserBooks(2);

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "The Frontiers Saga");

        assertEquals(SeriesAnswerStatus.OK, answer.status());
        assertEquals("The Frontiers Saga", answer.seriesTitle());
    }

    @Test
    void equallyGoodMatchesAreReportedAsAmbiguousWithOptions() {
        givenSeries(
                series(1, "The Frontiers Saga", LC_URL),
                series(2, "The Frontiers Saga Part 2: Rogue Castes", LC_URL));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "frontiers saga");

        assertEquals(SeriesAnswerStatus.AMBIGUOUS_SERIES, answer.status());
        assertEquals(List.of("The Frontiers Saga", "The Frontiers Saga Part 2: Rogue Castes"), answer.options());
    }

    @Test
    void shortWordsAreNotMatchedByPrefix() {
        givenSeries(series(1, "Starship", LC_URL), series(2, "Star Force", LC_URL));

        assertEquals(SeriesAnswerStatus.SERIES_NOT_FOUND, ask(BookQueryIntent.SERIES_ALL, "Star Trek").status());
        // "Star" pasuje tylko do "Star Force" (słowo równe), a nie do "Starship" (prefiks krótkiego słowa)
        assertEquals("Star Force", ask(BookQueryIntent.SERIES_ALL, "Star").seriesTitle());
    }

    @Test
    void unknownOrBlankSeriesNameGivesDedicatedStatuses() {
        givenBezimienny();

        assertEquals(SeriesAnswerStatus.SERIES_NOT_FOUND, ask(BookQueryIntent.SERIES_ALL, "Nieistniejący cykl").status());
        assertEquals(SeriesAnswerStatus.SERIES_NAME_MISSING, ask(BookQueryIntent.SERIES_ALL, "  ").status());
        assertEquals(SeriesAnswerStatus.SERIES_NAME_MISSING, ask(BookQueryIntent.SERIES_ALL, null).status());
    }

    // --- źródło tomów ---

    @Test
    void seriesWithoutSupportedSourceIsReportedWithItsTitle() {
        givenSeries(series(1, "Saga o wiedźminie", "https://upolujebooka.pl/cykl,72,saga_o_wiedzminie.html"));

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "Saga o wiedźminie");

        assertEquals(SeriesAnswerStatus.NO_SUPPORTED_SOURCE, answer.status());
        assertEquals("Saga o wiedźminie", answer.seriesTitle());
        assertTrue(catalog.requestedUrls.isEmpty());
    }

    @Test
    void sourceUrlListToleratesDuplicatesAndTrailingSeparator() {
        givenSeries(series(1, "Star Force", " ;;" + LC_URL + ";;" + LC_URL + ";;"));
        givenCatalog(vol("1", "Tom"));
        givenUserBooks(1);

        SeriesAnswer answer = ask(BookQueryIntent.SERIES_ALL, "Star Force");

        assertEquals(SeriesAnswerStatus.OK, answer.status());
        assertEquals(List.of(LC_URL), catalog.requestedUrls);
    }

    @Test
    void failingOrEmptySourceGivesSourceUnavailable() {
        givenBezimienny();
        catalog.failure = new IllegalStateException("403");
        assertEquals(SeriesAnswerStatus.SOURCE_UNAVAILABLE, ask(BookQueryIntent.SERIES_ALL, "Bezimienny").status());

        catalog.failure = null;
        catalog.volumes = List.of();
        assertEquals(SeriesAnswerStatus.SOURCE_UNAVAILABLE, ask(BookQueryIntent.SERIES_NEXT, "Bezimienny").status());
    }

    @Test
    void nonSeriesIntentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ask(BookQueryIntent.FIND_BOOK, "Bezimienny"));
    }
}
