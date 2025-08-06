package net.focik.homeoffice.library.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.domain.model.Bookstore;
import net.focik.homeoffice.library.domain.port.secondary.BookstoreRepository;
import net.focik.homeoffice.library.infrastructure.dto.BookstoreDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookstoreRepositoryAdapter implements BookstoreRepository {

    private final BookstoreDtoRepository bookstoreDtoRepository;
    private final ModelMapper mapper;

    @Override
    public Optional<Bookstore> add(Bookstore bookstore) {
        BookstoreDbDto bookstoreDbDto = mapper.map(bookstore, BookstoreDbDto.class);
        if (bookstoreDbDto.getId() == 0) {
            bookstoreDbDto.setId(null);
        }
        BookstoreDbDto saved = bookstoreDtoRepository.save(bookstoreDbDto);
        return Optional.of(mapper.map(saved, Bookstore.class));
    }

    @Override
    public Optional<Bookstore> edit(Bookstore bookstore) {
        BookstoreDbDto dbDto = bookstoreDtoRepository.save(mapper.map(bookstore, BookstoreDbDto.class));
        return Optional.of(mapper.map(dbDto, Bookstore.class));
    }

    @Override
    public void delete(Integer id) {
        bookstoreDtoRepository.deleteById(id);
    }

    @Override
    public Optional<Bookstore> findById(Integer id) {
        return bookstoreDtoRepository.findById(id)
                .map(bookstoreDbDto -> mapper.map(bookstoreDbDto, Bookstore.class));
    }

    @Override
    public Optional<Bookstore> findByName(String name) {
        return bookstoreDtoRepository.findBookstoreDtoByName(name)
                .map(bookstoreDbDto -> mapper.map(bookstoreDbDto, Bookstore.class));
    }

    @Override
    public List<Bookstore> findAll() {
        List<Bookstore> bookstoreList = new ArrayList<>();
        bookstoreDtoRepository.findAll()
                .iterator()
                .forEachRemaining(bookstoreDbDto -> bookstoreList.add(mapper.map(bookstoreDbDto, Bookstore.class)));
        return bookstoreList;
    }
}
