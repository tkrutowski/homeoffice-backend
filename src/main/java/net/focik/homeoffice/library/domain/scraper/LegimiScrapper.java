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
public class LegimiScrapper implements Scraper{

    private static final String PAGE_URL = "https://legimi.pl";

    public List<String> findBooksFromUrl(String url) {
        List<String> booksUrl = new ArrayList<>();
        try {
            if (!url.isEmpty()) {
                Document documentURL = Jsoup.connect(url).get();
                Element content = documentURL.getElementById("content");
                Elements links = content.getElementsByTag("a");
                return links.stream()
                        .map(element -> element.attr("href"))
                        .filter(s -> s.startsWith("/ebook"))
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
                Elements select = documentURL.select("ul.list-unstyled > li > a.category-link");
                if (!select.isEmpty() && select.getLast().parent().text().startsWith("Cykl:")) {
                    book.setSeries(select.getLast().text());
                    book.setSeriesURL(PAGE_URL + select.getLast().attr("href"));
                    book.setBookInSeriesNo("");
                }
            } catch (Exception e) {
                log.warn("Failed to parse series", e);
            }

            try {
                Element authorElement = documentURL.selectFirst("a.author-link.author-noseparator");
                if (authorElement != null) {
                    book.setAuthors(authorElement.text());
                }
            } catch (Exception e) {
                log.warn("Failed to parse authors", e);
            }

            try {
                String categories = documentURL.select("ul.list-unstyled > li > a.category-link").stream()
                        .filter(element -> element.parent().text().startsWith("Kategoria"))
                        .map(Element::text)
                        .collect(Collectors.joining(","));
                book.setCategories(categories);
            } catch (Exception e) {
                log.warn("Failed to parse categories", e);
            }

            try {
                Element titleElement = documentURL.selectFirst("h1.title-text");
                if (titleElement != null) {
                    book.setTitle(titleElement.text());
                }
            } catch (Exception e) {
                log.warn("Failed to parse title", e);
            }

            try {
                Element descElement = documentURL.selectFirst("#description_section");
                if (descElement != null) {
                    book.setDescription(descElement.text());
                }
            } catch (Exception e) {
                log.warn("Failed to parse description", e);
            }

            try {
                Element coverElement = documentURL.selectFirst("img.img-responsive.center-block");
                if (coverElement != null) {
                    String coverUrl = coverElement.attr("data-src");
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