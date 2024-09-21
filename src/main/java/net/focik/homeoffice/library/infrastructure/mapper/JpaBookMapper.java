package net.focik.homeoffice.library.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Category;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.infrastructure.dto.AuthorDbDto;
import net.focik.homeoffice.library.infrastructure.dto.BookDbDto;
import net.focik.homeoffice.library.infrastructure.dto.CategoryDbDto;
import net.focik.homeoffice.library.infrastructure.dto.SeriesDbDto;
import net.focik.homeoffice.library.infrastructure.jpa.AuthorDtoRepository;
import net.focik.homeoffice.library.infrastructure.jpa.CategoryDtoRepository;
import net.focik.homeoffice.library.infrastructure.jpa.SeriesDtoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JpaBookMapper {

    private final SeriesDtoRepository seriesDtoRepository;
    private final CategoryDtoRepository categoryDtoRepository;
    private final AuthorDtoRepository authorDtoRepository;
    private final ModelMapper mapper;

    public BookDbDto toDto(Book book) {
        return BookDbDto.builder()
                .id(book.getId())
                .series(getSeriesDbDto(book.getSeries()))
                .authors(getAuthorsDbDto(book.getAuthors()))
                .categories(getCategoriesDbDto(book.getCategories()))
                .title(book.getTitle())
                .description(book.getDescription())
                .cover(book.getCover())
                .bookInSeriesNo(book.getBookInSeriesNo())
                .build();
    }

    public Book toDomain(BookDbDto dto) {
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

    private SeriesDbDto getSeriesDbDto(Series series) {
        Optional<SeriesDbDto> seriesDtoByTitle = seriesDtoRepository.findSeriesDtoByTitle(series.getTitle());
        if (seriesDtoByTitle.isPresent()) {
            return seriesDtoByTitle.get();
        } else {
            SeriesDbDto seriesDto = new SeriesDbDto();
            seriesDto.setTitle(series.getTitle());
            seriesDto.setDescription(series.getDescription());
            seriesDto.setUrl(series.getUrl());
            return seriesDtoRepository.save(seriesDto);
        }
    }

    private Set<Category> getCategories(Set<CategoryDbDto> categories) {
        return categories.stream()
                .map(category -> mapper.map(category, Category.class))
                .collect(Collectors.toSet());
    }

    private Set<CategoryDbDto> getCategoriesDbDto(Set<Category> categories) {
        Set<CategoryDbDto> categoryDtos = new HashSet<>();
        for (Category category : categories) {
            Optional<CategoryDbDto> categoryDtoByName = categoryDtoRepository.findCategoryDtoByName(category.getName());
            if (categoryDtoByName.isPresent()) {
                categoryDtos.add(categoryDtoByName.get());
            } else {
                CategoryDbDto newCategoryDto = new CategoryDbDto();
                newCategoryDto.setName(category.getName());
                categoryDtos.add(categoryDtoRepository.save(newCategoryDto));
            }
        }
        return categoryDtos;
    }

    private Set<Author> getAuthors(Set<AuthorDbDto> authors) {
        return authors.stream()
                .map(author -> mapper.map(author, Author.class))
                .collect(Collectors.toSet());
    }

    private Set<AuthorDbDto> getAuthorsDbDto(Set<Author> authors) {
        Set<AuthorDbDto> authorDbDtos = new HashSet<>();
        for (Author author : authors) {
            Optional<AuthorDbDto> foundAuthor = authorDtoRepository.findAuthorDtoByFirstNameAndLastName(author.getFirstName(), author.getLastName());
            if (foundAuthor.isPresent()) {
                authorDbDtos.add(foundAuthor.get());
            } else {
                AuthorDbDto newAuthorDbDto = mapper.map(author, AuthorDbDto.class);
                authorDbDtos.add(authorDtoRepository.save(newAuthorDbDto));
            }
        }
        return authorDbDtos;
    }
}