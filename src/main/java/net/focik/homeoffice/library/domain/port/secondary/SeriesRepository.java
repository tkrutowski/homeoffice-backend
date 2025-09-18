package net.focik.homeoffice.library.domain.port.secondary;

import net.focik.homeoffice.library.domain.model.Series;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface SeriesRepository {

    Series add(Series series);

    Optional<Series> save(Series series);

    void delete(Integer id);

    Optional<Series> findById(Integer id);

    Optional<Series> findByTitle(String title);

    List<Series> findAll();
}
