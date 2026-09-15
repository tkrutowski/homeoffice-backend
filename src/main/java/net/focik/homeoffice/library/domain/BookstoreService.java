package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.BookNotFoundException;
import net.focik.homeoffice.library.domain.exception.BookstoreAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.BookstoreCanNotBeDeletedException;
import net.focik.homeoffice.library.domain.model.Bookstore;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.secondary.BookstoreRepository;
import net.focik.homeoffice.library.domain.port.secondary.UserBookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookstoreService {

    private final BookstoreRepository bookstoreRepository;
    private final UserBookRepository userBookRepository;

    public Bookstore addBookstore(Bookstore bookstore) {
        Optional<Bookstore> optionalBookstore = bookstoreRepository.findByName(bookstore.getName());
        if (optionalBookstore.isPresent()) {
            throw new BookstoreAlreadyExistException(bookstore);
        }
        return bookstoreRepository.add(bookstore).get();
    }

    public Bookstore updateBookstore(Bookstore bookstoreToEdit) {
        Optional<Bookstore> bookstoreById = bookstoreRepository.findById(bookstoreToEdit.getId());
        if (bookstoreById.isEmpty()) {
            throw new BookNotFoundException(bookstoreToEdit.getId());
        }

        bookstoreById.get().setName(bookstoreToEdit.getName());
        bookstoreById.get().setUrl(bookstoreToEdit.getUrl());

        return bookstoreRepository.edit(bookstoreById.get()).get();
    }

    public void deleteBookstore(Integer id) {
        List<UserBook> userBooksByBookstore = userBookRepository.findAllByBookstore(id);
        if (!userBooksByBookstore.isEmpty()) {
            log.warn("Bookstore with ID {} cannot be deleted — associated user books found (count: {}).", id, userBooksByBookstore.size());
            throw new BookstoreCanNotBeDeletedException("książki użytkowników. (" + userBooksByBookstore.size() + ")");
        }
        bookstoreRepository.delete(id);
    }

    public Bookstore findBookstore(Integer id) {
        Optional<Bookstore> bookstoreById = bookstoreRepository.findById(id);
        if (bookstoreById.isEmpty()) {
            throw new BookNotFoundException(id);
        }
        return bookstoreById.get();
    }

    public Bookstore findBookstoreByName(String name) {
        Optional<Bookstore> bookstoreByName = bookstoreRepository.findByName(name);
        if (bookstoreByName.isEmpty()) {
            throw new BookNotFoundException(name);
        }
        return bookstoreByName.get();
    }
    public List<Bookstore> findAllBookstores() {
        return bookstoreRepository.findAll();
    }
}