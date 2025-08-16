package net.focik.homeoffice.library.infrastructure.jpa;

import net.focik.homeoffice.library.infrastructure.dto.SeriesDbDto;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SeriesDtoRepository extends CrudRepository<SeriesDbDto, Integer> {
    Optional<SeriesDbDto> findSeriesDtoByTitleContainingIgnoreCase(String title);
}
