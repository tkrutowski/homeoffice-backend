package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.infrastructure.dto.BookstoreDbDto;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BookstoreDtoRepository extends CrudRepository<BookstoreDbDto, Integer> {
    Optional<BookstoreDbDto> findBookstoreDtoByNameContainingIgnoreCase(String name);
}
