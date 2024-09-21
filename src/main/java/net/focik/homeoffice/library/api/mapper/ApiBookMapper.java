package net.focik.homeoffice.library.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.api.dto.AuthorDto;
import net.focik.homeoffice.library.api.dto.BookApiDto;
import net.focik.homeoffice.library.api.dto.CategoryDto;
import net.focik.homeoffice.library.api.dto.SeriesDto;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Category;
import net.focik.homeoffice.library.domain.model.Series;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApiBookMapper {

    private final ApiAuthorMapper authorMapper;
    private final ModelMapper mapper;

    public BookApiDto toDto(Book book) {
        try {

        return BookApiDto.builder()
                .id(book.getId() != null ? book.getId() : 0)
                .series(book.getSeries() == null ? null :  mapper.map(book.getSeries(), SeriesDto.class))
                .authors(getAuthorsDto(book.getAuthors()))
                .categories(getCategoriesDto(book.getCategories()))
                .title(book.getTitle())
                .description(book.getDescription())
                .cover(book.getCover())
                .bookInSeriesNo(book.getBookInSeriesNo())
                .build();
        }catch (NullPointerException ex){
            System.out.println();
            return null;
        }
    }


    private List<AuthorDto> getAuthorsDto(Set<Author> authors) {
        return authors.stream()
                .map(authorMapper::toDto)
                .collect(Collectors.toList());
    }

    private List<CategoryDto> getCategoriesDto(Set<Category> categories) {
        return categories.stream()
                .map(category -> mapper.map(category, CategoryDto.class))
                .collect(Collectors.toList());
    }

    public Book toDomain(BookApiDto dto) {
        return Book.builder()
                .id(dto.getId())
                .series(Objects.nonNull(dto.getSeries()) ? mapper.map(dto.getSeries(), Series.class) : null)
                .authors(getAuthors(dto.getAuthors()))
                .categories(getCategories(dto.getCategories()))
                .title(dto.getTitle())
                .description(dto.getDescription())
                .cover(dto.getCover())
                .bookInSeriesNo(dto.getBookInSeriesNo())
                .build();
    }

    private Set<Author> getAuthors(List<AuthorDto> authors) {
        return authors.stream()
                .map(authorMapper::toDomain)
                .collect(Collectors.toSet());
    }

    private Set<Category> getCategories(List<CategoryDto> categories) {
        return categories.stream()
                .map(category -> mapper.map(category, Category.class))
                .collect(Collectors.toSet());
    }
//    private Series getSeriesFromString(String series, String seriesURL) {
//        Series s = new Series();
//        s.setTitle(series);
//        s.setUrl(seriesURL);
//        return s;
//    }


//    private Set<Author> getAuthorsFromString(String authors) {
//        Set<Author> authorDtos = new HashSet<>();
//        String[] authorsList = authors.trim().split(",");
//        for (String author : authorsList) {
//            authorDtos.add(validAuthor(author));
//        }
//        return authorDtos;
//    }
//
//    private Set<Category> getCategoriesFromString(String categories) {
//        Set<Category> categoryDtos = new HashSet<>();
//        String[] categoriesList = categories.trim().split(",");
//        for (String category : categoriesList) {
//            categoryDtos.add(new Category(0, category));
//        }
//        return categoryDtos;
//    }

//    private Author validAuthor(String author) {
//        Author authorDto = new Author();
//        String[] authorsSplit = author.trim().split(" ");
//
//        if (authorsSplit.length == 1) {
//            authorDto.setFirstName("");
//            authorDto.setLastName(authorsSplit[0]);
//        }
//        if (authorsSplit.length > 1) {
//            authorDto = author.lastIndexOf("-") > 0 ? getAuthorWithSeveralLastNames(author) : getAuthorWithSeveralFirstNames(author);
//        }
//        return authorDto;
//    }
//
//    private Author getAuthorWithSeveralFirstNames(String author) {
//        Author authorDto = new Author();
//        int i = author.lastIndexOf(" ");
//        authorDto.setFirstName(author.substring(0, i).trim());
//        authorDto.setLastName(author.substring(i).trim());
//        return authorDto;
//    }
//
//    private Author getAuthorWithSeveralLastNames(String author) {
//        Author authorDto;
//        int i = author.lastIndexOf("-");
//        authorDto = (getAuthorWithSeveralFirstNames(author.substring(0, i).trim()));
//
//        authorDto.setLastName(authorDto.getLastName() + "-" + author.substring(i + 1).trim());
//        return authorDto;
//    }
}