package net.focik.homeoffice.library.infrastructure.lubimyczytac;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.port.secondary.BookSearchPort;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wyszukiwanie w lubimyczytac.pl ({@code /szukaj/ksiazki?phrase=}). Ten serwis, w przeciwieństwie
 * do BookBeat, rozumie frazę "tytuł autor", więc najpierw pytamy o obie rzeczy, a dopiero gdy
 * to nic nie da (np. autor zapisany inaczej) ponawiamy z samym tytułem.
 */
@Slf4j
@Order(1)
@Component
public class LubimyczytacBookSearchAdapter implements BookSearchPort {

    static final String SOURCE = "lubimyczytac";
    private static final String PAGE_URL = "https://lubimyczytac.pl";
    private static final String SEARCH_URL = PAGE_URL + "/szukaj/ksiazki";
    // "Bezimienny (tom\n4)" -> seria "Bezimienny", numer "4"
    private static final Pattern SERIES = Pattern.compile("^\\s*(.*?)\\s*\\(\\s*tom\\s+([^)]*?)\\s*\\)\\s*$", Pattern.DOTALL);

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public List<BookSearchResult> search(String title, String author) {
        boolean hasAuthor = author != null && !author.isBlank();
        List<BookSearchResult> results = fetch(hasAuthor ? title + " " + author : title);
        if (results.isEmpty() && hasAuthor) {
            log.debug("Lubimyczytac: brak wyników dla '{} {}', ponawiam z samym tytułem", title, author);
            results = fetch(title);
        }
        return results;
    }

    private List<BookSearchResult> fetch(String phrase) {
        try {
            Document doc = Jsoup.connect(SEARCH_URL)
                    .data("phrase", phrase)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();
            List<BookSearchResult> results = parse(doc);
            if (results.isEmpty() && doc.selectFirst("#searcherNav") == null) {
                log.warn("Lubimyczytac: strona wyszukiwania nie ma oczekiwanej struktury - HTML mógł się zmienić");
            }
            return results;
        } catch (IOException e) {
            throw new IllegalStateException("Lubimyczytac search failed: " + e.getMessage(), e);
        }
    }

    static List<BookSearchResult> parse(Document doc) {
        List<BookSearchResult> results = new ArrayList<>();
        for (Element card : doc.select("div.book-card")) {
            Element titleEl = card.selectFirst("a.book-card__title");
            if (titleEl == null) {
                continue;
            }
            String href = titleEl.attr("href");
            if (href.isBlank()) {
                continue;
            }
            String title = titleEl.attr("title").isBlank() ? titleEl.text().trim() : titleEl.attr("title").trim();
            String url = href.startsWith("http") ? href : PAGE_URL + href;
            String authors = String.join(", ", card.select("div.book-card__author a").eachText());

            Element coverEl = card.selectFirst("img.book-card__cover-image");
            String cover = null;
            if (coverEl != null) {
                cover = coverEl.attr("src").isBlank() ? coverEl.attr("data-src") : coverEl.attr("src");
                if (cover.isBlank()) {
                    cover = null;
                }
            }

            String series = null;
            String number = null;
            Element cycleEl = card.selectFirst("div.book-card__detail--cycle a");
            if (cycleEl != null) {
                Matcher m = SERIES.matcher(cycleEl.text());
                if (m.matches()) {
                    series = m.group(1);
                    number = m.group(2);
                } else {
                    series = cycleEl.text().trim();
                }
            }

            results.add(new BookSearchResult(title, authors, cover, url, SOURCE, series, number));
        }
        return results;
    }
}
