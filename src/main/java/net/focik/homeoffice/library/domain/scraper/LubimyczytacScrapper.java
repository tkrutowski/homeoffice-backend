package net.focik.homeoffice.library.domain.scraper;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookDto;
import net.focik.homeoffice.utils.StringHelper;
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
public class LubimyczytacScrapper implements Scraper{

    private static final String PAGE_URL = "https://lubimyczytac.pl";

    public List<String> findBooksFromUrl(String url) {
        List<String> booksUrl = new ArrayList<>();
        try {
            if (!url.isEmpty()) {
                Document documentURL = Jsoup.connect(url).get();
                Element content = documentURL.getElementById("seriesFilteredList");
                Elements links = content.getElementsByTag("a");
                return links.stream()
                        .map(element -> element.attr("href"))
                        .filter(s -> s.startsWith("/ksiazka"))
                        .distinct()
                        .map(PAGE_URL::concat)
                        .collect(Collectors.toList());
            }
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }

        return booksUrl;
    }

    public BookDto findBookFromUrl(String url) {
        BookScraperDto book = new BookScraperDto();
        book.setId(0);

        if (url.isEmpty()) {
            return book;
        }

        try {
            Document documentURL = Jsoup.connect(url).get();

            try {
                Elements select = documentURL.select("span.d-none.d-sm-block.mt-1");
                if (!select.isEmpty() && select.getFirst().text().startsWith("Cykl:")) {
                    book.setSeries(StringHelper.extractTextBetweenColonAndParenthesis(select.getFirst().text()));
                    book.setSeriesURL(PAGE_URL + select.getFirst().children().getFirst().attr("href"));
                    book.setBookInSeriesNo(StringHelper.extractNumberFromParentheses(select.getFirst().text()));
                }
            } catch (Exception e) {
                log.warn("Failed to parse series", e);
            }

            try {
                Element authorsElement = documentURL.selectFirst("span.author.pb-2");
                if (authorsElement != null) {
                    String authors = authorsElement.children().stream()
                            .map(Element::text)
                            .collect(Collectors.joining(", "));
                    book.setAuthors(authors);
                }
            } catch (Exception e) {
                log.warn("Failed to parse authors", e);
            }

            try {
                Element categoryElement = documentURL.selectFirst("a.book__category.d-sm-block.d-none");
                if (categoryElement != null) {
                    book.setCategories(categoryElement.text());
                }
            } catch (Exception e) {
                log.warn("Failed to parse categories", e);
            }

            try {
                Element titleElement = documentURL.selectFirst("h1.book__title");
                if (titleElement != null) {
                    book.setTitle(titleElement.text());
                }
            } catch (Exception e) {
                log.warn("Failed to parse title", e);
            }

            try {
                Element descDiv = documentURL.selectFirst("div#book-description");
                if (descDiv != null) {
                    book.setDescription(descDiv.text());
                }
            } catch (Exception e) {
                log.warn("Failed to parse description", e);
            }

            try {
                Element coverLink = documentURL.selectFirst("#js-lightboxCover");
                if (coverLink != null) {
                    String coverUrl = coverLink.attr("href");
                    if (coverUrl.isEmpty()) {
                        coverUrl = coverLink.attr("data-cover");
                    }
                    if (!coverUrl.isEmpty()) {
                        book.setCover(coverUrl);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse cover", e);
            }

        } catch (Exception exception) {
            log.error("Failed to fetch document from URL: {}", url, exception);
        }

        return book;
    }
}