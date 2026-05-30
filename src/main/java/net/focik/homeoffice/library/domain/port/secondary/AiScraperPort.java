package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.scraper.BookScraperDto;

public interface AiScraperPort {
    BookScraperDto findBookByUrl(String url);
}
