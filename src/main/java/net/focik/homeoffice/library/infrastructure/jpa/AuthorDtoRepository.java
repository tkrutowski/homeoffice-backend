package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.infrastructure.dto.AuthorDbDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuthorDtoRepository extends JpaRepository<AuthorDbDto, Integer> {
    Optional<AuthorDbDto> findAuthorDtoByFirstNameAndLastNameIgnoreCase(String firstName, String lastName);

    List<AuthorDbDto> findAllByOrderByLastNameAsc();

    @Query("SELECT a FROM AuthorDbDto a WHERE " +
            "(:globalFilter IS NULL) OR " +
            "(LOWER(a.firstName) LIKE LOWER(CONCAT('%', :globalFilter, '%')) OR " +
            "LOWER(a.lastName) LIKE LOWER(CONCAT('%', :globalFilter, '%')))")
    Page<AuthorDbDto> findAuthorsWithFilters(@Param("globalFilter") String globalFilter, Pageable pageable);
}