package net.focik.homeoffice.library.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import net.focik.homeoffice.library.infrastructure.dto.BookDbDto;
import net.focik.homeoffice.library.infrastructure.dto.SeriesDbDto;
import net.focik.homeoffice.library.infrastructure.mapper.JpaBookMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class BookRepositoryAdapter implements BookRepository {

    private final BookDtoRepository bookDtoRepository;
    private final JpaBookMapper bookMapper;
    private final ModelMapper mapper;

    @Override
    public Optional<Book> add(Book book) {
        BookDbDto bookDbDtoToSave = bookMapper.toDto(book);
        if (bookDbDtoToSave.getId() == 0){
            bookDbDtoToSave.setId(null);
        }
        BookDbDto savedBook = bookDtoRepository.save(bookDbDtoToSave);
        return Optional.of(savedBook)
                .map(bookMapper::toDomain);
    }

    @Override
    public Optional<Book> update(Book book) {
        return add(book);
    }

    @Override
    public void delete(Integer id) {
        bookDtoRepository.deleteById(id);
    }

    @Override
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        bookDtoRepository.findAllByOrderByIdDesc()
                .iterator()
                .forEachRemaining(dto -> books.add(bookMapper.toDomain(dto)));
        return books;
    }

    @Override
    public Optional<Book> findById(Integer id) {
        return bookDtoRepository.findById(id)
                .map(bookMapper::toDomain);
    }

    @Override
    public List<Book> findAllByTitle(String title) {
        List<Book> books = new ArrayList<>();
        bookDtoRepository.findAllByTitleIgnoreCase(title)
                .iterator()
                .forEachRemaining(bookDto -> books.add(bookMapper.toDomain(bookDto)));
        return books;
    }

    @Override
    public List<Book> findAllBySeries(Series series) {
        List<Book> books = new ArrayList<>();
        bookDtoRepository.findAllBySeriesOrderByBookInSeriesNo(mapper.map(series, SeriesDbDto.class))
                .iterator()
                .forEachRemaining(dto -> books.add(bookMapper.toDomain(dto)));
//
//         books = booksDto.stream()
//                 .filter(bookDto -> bookDto.getSeries().equals(SeriesDbDto.fromDomain(series)))
//                 .map(BookDbDto::toDomain).collect(Collectors.toList());
        return books;
    }

    @Override
    public Optional<Book> findByTitle(String title) {
        return Optional.empty();
    }
}
