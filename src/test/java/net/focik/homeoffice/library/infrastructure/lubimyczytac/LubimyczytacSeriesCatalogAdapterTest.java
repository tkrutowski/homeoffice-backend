package net.focik.homeoffice.library.infrastructure.lubimyczytac;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LubimyczytacSeriesCatalogAdapterTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private final LubimyczytacSeriesCatalogAdapter adapter = new LubimyczytacSeriesCatalogAdapter(MAPPER);

    // Dwie karty w kształcie z odpowiedzi /series/getMoreBooks (numer 0 i ułamkowy 15.5 jak w cyklu Jack Reacher)
    private static final String CARDS_HTML = """
            <div class="book-card book-card--l " data-book-id="1"><div class="book-card__primary"><div class="book-card__info-box">
              <a class="book-card__title" title="Jack Reacher. Zasady" href="/ksiazka/4809402/jack-reacher-zasady"> Jack Reacher. Zasady </a>
              <div class="book-card__author"><a href="/autor/15911/lee-child">Lee Child</a></div>
              <div class="book-card__detail book-card__detail--cycle"> Cykl:
                <a class="book-card__highlighted" href="/cykl/1233/jack-reacher"> Jack Reacher (tom
                  0)
                </a></div>
            </div></div></div>
            <div class="book-card book-card--l " data-book-id="2"><div class="book-card__primary"><div class="book-card__info-box">
              <a class="book-card__title" title="Deep Down" href="/ksiazka/1/deep-down"> Deep Down </a>
              <div class="book-card__author"><a href="/autor/15911/lee-child">Lee Child</a></div>
              <div class="book-card__detail book-card__detail--cycle"> Cykl:
                <a class="book-card__highlighted" href="/cykl/1233/jack-reacher"> Jack Reacher (tom
                  15.5)
                </a></div>
            </div></div></div>
            """;

    private static String json(Object data) {
        return MAPPER.writeValueAsString(Map.of("code", "OK", "data", data));
    }

    @Test
    void supportsSeriesAndPublisherSeriesUrlsOnlyFromLubimyczytac() {
        assertTrue(adapter.supports("https://lubimyczytac.pl/cykl/32783/bezimienny"));
        assertTrue(adapter.supports("https://www.lubimyczytac.pl/cykl/1233/jack-reacher"));
        assertTrue(adapter.supports("https://lubimyczytac.pl/seria/28336/astrofizyka-w-kieszeni"));
        assertTrue(adapter.supports("  https://lubimyczytac.pl/cykl/38622/luta-karabina  "));

        assertFalse(adapter.supports("https://upolujebooka.pl/cykl,198,joanna_chylka.html"));
        assertFalse(adapter.supports("https://www.legimi.pl/ebooki/jack-reacher,c968/"));
        assertFalse(adapter.supports("https://frontierssaga.com/episodes/"));
        assertFalse(adapter.supports("https://lubimyczytac.pl/ksiazka/5133755/rodzinny-interes"));
        assertFalse(adapter.supports(null));
    }

    @Test
    void parsesCardsAndRemainingCountFromApiResponse() {
        var page = LubimyczytacSeriesCatalogAdapter.parsePage(
                json(Map.of("count", 46, "left", 6, "content", CARDS_HTML)), MAPPER);

        assertEquals(6, page.left());
        assertEquals(2, page.volumes().size());
        var first = page.volumes().get(0);
        assertEquals("Jack Reacher. Zasady", first.title());
        assertEquals("Lee Child", first.authors());
        assertEquals("0", first.bookInSeriesNo());
        assertEquals("Jack Reacher", first.series());
        assertEquals("https://lubimyczytac.pl/ksiazka/4809402/jack-reacher-zasady", first.url());
        assertEquals("15.5", page.volumes().get(1).bookInSeriesNo());
    }

    @Test
    void emptyDataArrayFromErrorResponseGivesEmptyPage() {
        // przy błędzie serwis zwraca "data": [] zamiast obiektu
        var page = LubimyczytacSeriesCatalogAdapter.parsePage(json(java.util.List.of()), MAPPER);

        assertTrue(page.volumes().isEmpty());
        assertEquals(0, page.left());
    }

    @Test
    void findVolumesRejectsUnsupportedUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.findVolumes("https://upolujebooka.pl/cykl,198,joanna_chylka.html"));
    }
}
