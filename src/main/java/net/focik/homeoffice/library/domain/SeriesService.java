package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.exception.SeriesAlreadyExistException;
import net.focik.homeoffice.library.domain.exception.SeriesNotFoundException;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.secondary.SeriesRepository;
import net.focik.homeoffice.utils.exceptions.ObjectNotSavedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;

    public Integer addSeries(Series series) {
        Optional<Series> optionalSeries = seriesRepository.findByTitle(series.getTitle());
        if (optionalSeries.isPresent()) {
            throw new SeriesAlreadyExistException(series);
        }
        return seriesRepository.add(series);
    }

    public Series editSeries(Series series, Integer id) {
        Optional<Series> seriesById = seriesRepository.findById(id);
        if (seriesById.isEmpty()) {
            throw new SeriesNotFoundException(id);
        }
        seriesById.get().setTitle(series.getTitle());
        seriesById.get().setDescription(series.getDescription());

        return seriesRepository.save(seriesById.get()).get();
    }

    public Series saveSeries(Series series) {

        return seriesRepository.save(series).orElse(null);
    }

    public void deleteSeries(Integer id) {
        seriesRepository.delete(id);
    }

    public Series findSeries(Integer id) {
        log.debug("Trying to find series with id: {}", id);
        Optional<Series> seriesOptional = seriesRepository.findById(id);
        if (seriesOptional.isEmpty()) {
            log.warn("Series with id: {} not found", id);
            throw new SeriesNotFoundException(id);
        }
        log.debug("Found series with id: {}", id);
        return seriesOptional.get();
    }

    public Series findSeriesByTitle(String title) {
        Optional<Series> seriesOptional = seriesRepository.findByTitle(title);
        if (seriesOptional.isEmpty()) {
            log.warn("Series with title: {} not found", title);
            throw new SeriesNotFoundException(title);
        }
        return seriesOptional.get();
    }

    public List<Series> getAllSeries() {
        return seriesRepository.findAll().stream()
                .filter(series -> Objects.nonNull(series.getTitle()))
                .sorted(Comparator.comparing(Series::getTitle))
                .collect(Collectors.toList());
    }

    public Series validSeries(Series series) {
        if (Objects.isNull(series)) {
            return null;
        }
        List<Series> all = seriesRepository.findAll();
        String title = series.getTitle().trim();
        Optional<Series> first = all.stream()
                .filter(serie -> StringUtils.containsIgnoreCase(serie.getTitle(), title))
                .findFirst();
        if (first.isPresent() && !first.get().getUrl().equals(series.getUrl())) {
            String tempUrl = first.get().getUrl() + ";;" + series.getUrl();
            first.get().setUrl(tempUrl);
        }
        return first.orElse(series);
    }

    public Series updateSeries(Series series) {
        return seriesRepository.save(series).orElseThrow(() -> {
            log.error("Series with title: {} not saved", series.getTitle());
            return new ObjectNotSavedException("Cykl nie zapisany: " + series.getTitle());
        });
    }
}
