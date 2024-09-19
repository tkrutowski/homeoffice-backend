package net.focik.homeoffice.library.domain.scraper;

import net.focik.homeoffice.library.domain.model.BookDto;

import java.util.List;

public interface Scraper {
    List<String> findBooksFromUrl(String url);
    BookDto findBookFromUrl(String url);
}
