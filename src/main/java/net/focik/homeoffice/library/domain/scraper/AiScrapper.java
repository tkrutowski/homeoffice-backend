package net.focik.homeoffice.library.domain.scraper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.domain.model.BookDto;
import net.focik.homeoffice.library.domain.port.secondary.AiScraperPort;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

@RequiredArgsConstructor
public class AiScrapper implements Scraper {

    private final AiScraperPort aiScraperPort;

    @Override
    public List<String> findBooksFromUrl(String url) {
        throw new NotImplementedException();
    }

    @Override
    public BookDto findBookFromUrl(String url) {
        return aiScraperPort.findBookByUrl(url);
    }
}
