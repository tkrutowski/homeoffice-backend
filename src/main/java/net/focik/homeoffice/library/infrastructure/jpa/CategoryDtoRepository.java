package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.infrastructure.dto.CategoryDbDto;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CategoryDtoRepository extends CrudRepository<CategoryDbDto, Integer> {

    Optional<CategoryDbDto> findCategoryDtoByNameIgnoreCase(String name);
}
