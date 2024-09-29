package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.*;
import net.focik.homeoffice.library.domain.scraper.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class BookScraperService {

    public Book findBookByUrl(String url) {
        log.debug("Trying to find book by url {}", url);
        Scraper scraper  = getScraperFromUrl(url);
        log.debug("Got scraper: {}", scraper);
        BookDto bookDto = scraper.findBookFromUrl(url);
        if (StringUtils.isEmpty(((BookScraperDto)bookDto).getTitle())) {
            return null;
        }
        log.debug("Found book by url {}", url);
        return convertToBook((BookScraperDto) bookDto);
    }

    private Scraper getScraperFromUrl(String url) {
        log.debug("Trying to get scraper from url {}", url);
        if(url.contains(WebSite.LUBIMY_CZYTAC.getUrl()))
            return new LubimyczytacScrapper();
        else  if(url.contains(WebSite.UPOLUJ_EBOOKA.getUrl()))
            return new UpolujebookaScrapper();
        else  if(url.contains(WebSite.LEGIMI.getUrl()))
            return  new LegimiScrapper();
        else return new AiScrapper();
    }

    public List<Book> findBooksInSeries(String url, String existingTitles) {
        log.debug("Trying to find books in series by url {}", url);
        Scraper scraper  = getScraperFromUrl(url);
        List<String> booksFromUrl = scraper.findBooksFromUrl(url);
        return booksFromUrl.stream()
                .map(this::findBookByUrl)
                .filter(Objects::nonNull)
                .filter(book -> !existingTitles.toLowerCase().contains(book.getTitle().toLowerCase()))
                .collect(Collectors.toList());
    }

    private Book convertToBook(BookScraperDto dto) {
        log.debug("Trying convert to Book: {}", dto);
        return Book.builder()
                .id(dto.getId())
                .series(getSeriesFromString(dto.getSeries(), dto.getSeriesURL()))
                .authors(getAuthorsFromString(dto.getAuthors()))
                .categories(getCategoriesFromString(dto.getCategories()))
                .title(dto.getTitle())
                .description(dto.getDescription())
                .cover(dto.getCover())
                .bookInSeriesNo(dto.getBookInSeriesNo())
                .build();
    }

    private Series getSeriesFromString(String series, String seriesURL) {
        log.debug("Trying to get series from series url {}", seriesURL);
        if (StringUtils.isEmpty(series))
            return null;
        Series s = new Series();
        s.setTitle(series);
        s.setUrl(seriesURL);
        log.debug("Got series from series url {}", s);
        return s;
    }


    private Set<Author> getAuthorsFromString(String authors) {
        log.debug("Trying to get authors from authors url {}", authors);
        Set<Author> authorDtos = new HashSet<>();
        String[] authorsList = authors.trim().split(",");
        for (String author : authorsList) {
            authorDtos.add(validAuthor(author));
        }
        log.debug("Got authors from authors url {}", authorDtos);
        return authorDtos;
    }

    private Set<Category> getCategoriesFromString(String categories) {
        log.debug("Trying to get categories from categories url {}", categories);
        Set<Category> categoryDtos = new HashSet<>();
        String[] categoriesList = categories.trim().split(",");
        for (String category : categoriesList) {
            categoryDtos.add(new Category(0, category.trim()));
        }
        log.debug("Got categories from categories url {}", categoryDtos);
        return categoryDtos;
    }

    private Author validAuthor(String author) {
        log.debug("Trying to validate author {}", author);
        Author authorDto = new Author();
        String[] authorsSplit = author.trim().split(" ");

        if (authorsSplit.length == 1) {
            authorDto.setFirstName("");
            authorDto.setLastName(authorsSplit[0]);
        }
        if (authorsSplit.length > 1) {
            authorDto = author.lastIndexOf("-") > 0 ? getAuthorWithSeveralLastNames(author) : getAuthorWithSeveralFirstNames(author);
        }
        log.debug("Got validated author {}", authorDto);
        return authorDto;
    }

    private Author getAuthorWithSeveralFirstNames(String author) {
        log.debug("Trying to validate author with several first names: {}", author);
        Author authorDto = new Author();
        int i = author.lastIndexOf(" ");
        authorDto.setFirstName(author.substring(0, i).trim());
        authorDto.setLastName(author.substring(i).trim());
        log.debug("Got author {}", authorDto);
        return authorDto;
    }

    private Author getAuthorWithSeveralLastNames(String author) {
        log.debug("Trying to validate author with several last names: {}", author);
        Author authorDto;
        int i = author.lastIndexOf("-");
        authorDto = (getAuthorWithSeveralFirstNames(author.substring(0, i).trim()));

        authorDto.setLastName(authorDto.getLastName() + "-" + author.substring(i + 1).trim());
        log.debug("Got author {}", authorDto);
        return authorDto;
    }
}
