package net.focik.homeoffice.library.domain.scraper;

import net.focik.homeoffice.library.domain.exception.ScraperBlockedException;
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
            throw new ScraperBlockedException(exception.getMessage());
        }

        return booksUrl;
    }

    public BookDto findBookFromUrl(String url) {
        BookScraperDto book = new BookScraperDto();
        book.setId(0);
        try {
            if (!url.isEmpty()) {
                Document documentURL = Jsoup.connect(url).get();

                Elements select = documentURL.select("span.d-none.d-sm-block.mt-1");
                if (!select.isEmpty() && select.getFirst().text().startsWith("Cykl:")) {
                    book.setSeries(StringHelper.extractTextBetweenColonAndParenthesis(select.getFirst().text()));
                    book.setSeriesURL(PAGE_URL + select.getFirst().children().getFirst().attr("href"));
                    book.setBookInSeriesNo(StringHelper.extractNumberFromParentheses(select.getFirst().text()));
                }

                book.setAuthors(documentURL.select("span.author.pb-2").getFirst().children().stream()
                        .map(Element::text)
                        .collect(Collectors.joining(", ")));

                book.setCategories(documentURL.select("a.book__category.d-sm-block.d-none").getFirst().text());
                book.setTitle(documentURL.select("h1.book__title").getFirst().text());
                book.setDescription(documentURL.select("div.collapse-content > p").getFirst().text());
                book.setCover(documentURL.select("#js-lightboxCover").getFirst().attr("href"));
            }

        } catch (Exception exception) {
            throw new ScraperBlockedException(exception.getMessage());
        }
        return book;
    }



}