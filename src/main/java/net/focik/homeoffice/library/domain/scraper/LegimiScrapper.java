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
        try {
            if (!url.isEmpty()) {
                Document documentURL = Jsoup.connect(url).get();

                Elements select = documentURL.select("ul.list-unstyled > li > a.category-link");
                if (!select.isEmpty() && select.getLast().parent().text().startsWith("Cykl:")) {
                    book.setSeries(select.getLast().text());
                    book.setSeriesURL(PAGE_URL + select.getLast().attr("href"));
                    book.setBookInSeriesNo("");
                }

                book.setAuthors(documentURL.select("a.author-link.author-noseparator").getFirst().text());

                book.setCategories(documentURL.select("ul.list-unstyled > li > a.category-link").stream()
                        .filter(element -> element.parent().text().startsWith("Kategoria"))
                        .map(Element::text)
                        .collect(Collectors.joining(",")));
                book.setTitle(documentURL.select("h1.title-text").text());
                book.setDescription(documentURL.select("#description_section").text());
                book.setCover(documentURL.select("img.img-responsive.center-block").attr("data-src"));
            }

        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
        return book;
    }



}