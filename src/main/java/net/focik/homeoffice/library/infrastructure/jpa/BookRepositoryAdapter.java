package net.focik.homeoffice.library.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import net.focik.homeoffice.library.infrastructure.dto.BookDbDto;
import net.focik.homeoffice.library.infrastructure.dto.SeriesDbDto;
import net.focik.homeoffice.library.infrastructure.mapper.JpaBookMapper;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        if (bookDbDtoToSave.getId() != null && bookDbDtoToSave.getId() == 0) {
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
    public Page<Book> findAll(Pageable pageable) {
        Page<BookDbDto> all = bookDtoRepository.findAll(pageable);
        return all.map(bookMapper::toDomain);
    }

    @Override
    public Page<Book> findBooksWithFilters(String globalFilter, String title, String author, String category, String series, Pageable pageable) {
        Page<BookDbDto> booksPage = bookDtoRepository.findBooksWithFilters(
                globalFilter, title, author, category, series, pageable
        );
        return booksPage.map(bookMapper::toDomain);
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
        return books;
    }

    @Override
    public Optional<Book> findByTitle(String title) {
        return Optional.empty();
    }

    @Override
    public Long countBooksByAuthorId(Integer authorId) {
        return bookDtoRepository.countBooksByAuthorId(authorId);
    }

    @Override
    public List<Book> findAllByAuthor(Integer authorId) {
        List<Book> books = new ArrayList<>();
        bookDtoRepository.findAllByAuthorIdOrderByTitle(authorId)
                .iterator()
                .forEachRemaining(dto -> books.add(bookMapper.toDomain(dto)));
        return books;
    }
}
