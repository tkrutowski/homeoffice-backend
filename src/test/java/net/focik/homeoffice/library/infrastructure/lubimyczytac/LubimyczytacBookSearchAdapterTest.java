package net.focik.homeoffice.library.infrastructure.lubimyczytac;

import net.focik.homeoffice.library.domain.model.BookSearchResult;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LubimyczytacBookSearchAdapterTest {

    // Fragment prawdziwej odpowiedzi /szukaj/ksiazki (karta z cyklem) + druga karta bez cyklu i z dwoma autorami
    private static final String HTML = """
            <div id="searcherNav"></div>
            <div id="ksiazki">
            <div
            class="book-card book-card--l "
            id="listBookElement5133755" data-book-id="5133755"><div class="book-card__primary"><div class="book-card__cover-wrapper"><form class="formToHref book-card__cover-link" action="/ksiazka/5133755/rodzinny-interes" method="post"><img class="book-card__cover-image" src="https://s.lubimyczytac.pl/upload/books/5133000/5133755/1187730-170x243.jpg" alt="x" /></form></div><div class="book-card__info-box"><a class="book-card__title" title="Rodzinny interes" href="/ksiazka/5133755/rodzinny-interes"> Rodzinny interes </a><div class="book-card__author"><a href="https://lubimyczytac.pl/autor/75284/wojciech-chmielarz">Wojciech Chmielarz</a></div><div class="book-card__detail book-card__detail--cycle"> Cykl:<a class="book-card__highlighted book-card__highlighted--link" href="/cykl/32783/bezimienny"> Bezimienny (tom
            4)</a></div></div></div></div>
            <div class="book-card book-card--l " data-book-id="2"><div class="book-card__primary"><div class="book-card__info-box"><a class="book-card__title" title="" href="/ksiazka/2/dwoch-autorow"> Dwóch autorów </a><div class="book-card__author"><a href="/autor/1/jan-kowalski">Jan Kowalski</a><a href="/autor/2/anna-nowak">Anna Nowak</a></div></div></div></div>
            <div class="book-card book-card--l "><div class="book-card__primary"><div class="book-card__info-box"><span>karta bez tytułu - pomijana</span></div></div></div>
            </div>
            """;

    @Test
    void parsesTitleAuthorCoverSeriesAndAbsoluteUrl() {
        List<BookSearchResult> results = LubimyczytacBookSearchAdapter.parse(Jsoup.parse(HTML));

        assertEquals(2, results.size());
        BookSearchResult first = results.get(0);
        assertEquals("Rodzinny interes", first.title());
        assertEquals("Wojciech Chmielarz", first.authors());
        assertEquals("https://lubimyczytac.pl/ksiazka/5133755/rodzinny-interes", first.url());
        assertEquals("https://s.lubimyczytac.pl/upload/books/5133000/5133755/1187730-170x243.jpg", first.cover());
        assertEquals("Bezimienny", first.series());
        assertEquals("4", first.bookInSeriesNo());
        assertEquals("lubimyczytac", first.source());
    }

    @Test
    void fallsBackToLinkTextWhenTitleAttributeEmptyAndJoinsAuthors() {
        BookSearchResult second = LubimyczytacBookSearchAdapter.parse(Jsoup.parse(HTML)).get(1);

        assertEquals("Dwóch autorów", second.title());
        assertEquals("Jan Kowalski, Anna Nowak", second.authors());
        assertNull(second.cover());
        assertNull(second.series());
        assertNull(second.bookInSeriesNo());
    }

    @Test
    void returnsEmptyListWhenNoCards() {
        assertTrue(LubimyczytacBookSearchAdapter.parse(Jsoup.parse("<html><body>nic</body></html>")).isEmpty());
    }
}
