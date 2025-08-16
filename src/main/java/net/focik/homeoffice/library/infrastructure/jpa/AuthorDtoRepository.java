package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.infrastructure.dto.AuthorDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorDtoRepository extends JpaRepository<AuthorDbDto, Integer> {
    Optional<AuthorDbDto> findAuthorDtoByFirstNameAndLastNameIgnoreCase(String firstName, String lastName);

    List<AuthorDbDto> findAllByOrderByLastNameAsc();
}