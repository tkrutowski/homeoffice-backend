package net.focik.homeoffice.library.domain.scraper;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UpolujebookaScrapper implements Scraper{

    public static final String DIV_PUBLISHER = "div.publisher";

    private static final String PAGE_URL = "https://upolujebooka.pl";

    public List<String> findBooksFromUrl(String url) {
        List<String> booksUrl = new ArrayList<>();
        try {
            if (!url.isEmpty()) {
                Document documentURL = Jsoup.connect(url).get();
                return documentURL.select("a[href*=/oferta]").stream()
                        .map(element -> element.attr("href"))
                        .filter(s -> s.startsWith("/oferta"))
                        .distinct()
                        .map(PAGE_URL::concat)
                        .collect(Collectors.toList());
            }
        }catch (Exception exception) {
            log.error(exception.getMessage());
        }

        return booksUrl;
    }

    public BookDto findBookFromUrl(String url) {
        BookScraperDto book = new BookScraperDto();
        book.setId(0);
        try {
            if (!url.isEmpty()) {
                Document documentURL = Jsoup.connect(url).get();
                Element seriesSection = documentURL.selectFirst("div.mt-4:has(div:containsOwn(Cykl:))");
                if (seriesSection != null) {
                    Element seriesLink = seriesSection.selectFirst("a");
                    if (seriesLink != null) {
                        Elements spans = seriesLink.select("span");
                        book.setSeries(spans.get(0).text());
                        book.setSeriesURL(PAGE_URL + seriesLink.attr("href"));
                        book.setBookInSeriesNo(spans.size() > 1 ? spans.get(1).text() : "");
                    }
                } else {
                    book.setSeries("");
                }
                book.setAuthors(String.join(", ", documentURL.select("h1 a[itemprop=author]").eachText()));
                book.setCategories(String.join(",", documentURL.select("div.mt-2 a[href*=kategoria]").eachText()));
                Element h1 = documentURL.selectFirst("h1");
                if (h1 != null) {
                    String fullTitle = h1.text();
                    String title = fullTitle.split("\\s*-\\s*")[0];
                    book.setTitle(title);
                }
                Element descriptionDiv = documentURL.selectFirst("div.line-clamp-5");
                if (descriptionDiv != null) {
                    book.setDescription(descriptionDiv.text());
                }
                Element coverImg = documentURL.selectFirst("button#open-modal img");
                if (coverImg != null) {
                    String src = coverImg.attr("src");
                    if (!src.startsWith("http")) {
                        src = PAGE_URL + "/" + src;
                    }
                    book.setCover(src);
                }

            }
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
        return book;
    }

}