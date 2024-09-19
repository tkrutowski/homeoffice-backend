package net.focik.homeoffice.library.domain.scraper;

import net.focik.homeoffice.library.domain.model.BookDto;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiScrapper implements Scraper{

    private static final String PAGE_URL = "https://legimi.pl";

    public List<String> findBooksFromUrl(String url) {
       throw new NotImplementedException();
    }

    public BookDto findBookFromUrl(String url) {
        throw new NotImplementedException();
    }
}