package net.focik.homeoffice.library.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.secondary.SeriesRepository;
import net.focik.homeoffice.library.infrastructure.dto.SeriesDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SeriesRepositoryAdapter implements SeriesRepository {

    private final SeriesDtoRepository seriesDtoRepository;
    private final ModelMapper mapper;

    @Override
    public Integer add(Series series) {
        SeriesDbDto seriesDbDto = mapper.map(series, SeriesDbDto.class);
        if (seriesDbDto.getId() == 0){
            seriesDbDto.setId(null);
        }
        return seriesDtoRepository.save(seriesDbDto).getId();
    }

    @Override
    public Optional<Series> save(Series series) {
        SeriesDbDto dbDto = seriesDtoRepository.save(mapper.map(series, SeriesDbDto.class));
        return Optional.of(mapper.map(dbDto, Series.class));
    }

    @Override
    public void delete(Integer id) {
        seriesDtoRepository.deleteById(id);
    }

    @Override
    public Optional<Series> findById(Integer id) {
        return seriesDtoRepository.findById(id)
                .map(seriesDbDto -> mapper.map(seriesDbDto, Series.class));
    }

    @Override
    public Optional<Series> findByTitle(String title) {
        return seriesDtoRepository.findSeriesDtoByTitleContainingIgnoreCase(title)
                .map(seriesDbDto -> mapper.map(seriesDbDto, Series.class));
    }

    @Override
    public List<Series> findAll() {
        List<Series> seriesList = new ArrayList<>();
        seriesDtoRepository.findAll()
                .iterator()
                .forEachRemaining(seriesDbDto -> seriesList.add(mapper.map(seriesDbDto, Series.class)));
        return seriesList;
    }
}