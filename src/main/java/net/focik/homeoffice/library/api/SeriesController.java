package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.api.dto.SeriesDto;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.port.primary.FindSeriesUseCase;
import net.focik.homeoffice.library.domain.port.primary.SaveSeriesUseCase;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/series")
public class SeriesController {

    private final FindSeriesUseCase findSeriesUseCase;
    private final SaveSeriesUseCase saveSeriesUseCase;
    private final ModelMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<SeriesDto>> getAllSeries() {
        log.info("Request to get all series");
        List<Series> allSeries = findSeriesUseCase.getAllSeries();
        if (allSeries.isEmpty()) {
            log.warn("No series found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found {} series", allSeries.size());
        return new ResponseEntity<>(allSeries.stream()
                .peek(series -> log.debug("Found series {}", series))
                .map(series -> mapper.map(series, SeriesDto.class))
                .peek(dto -> log.debug("Mapped found series {}", dto))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<SeriesDto> getSeries(@PathVariable Integer id) {
        log.info("Request to get series with id: {}", id);
        Series series = findSeriesUseCase.findSeries(id);
        if (series == null) {
            log.warn("No series found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found series: {}", series);
        SeriesDto dto = mapper.map(series, SeriesDto.class);
        log.info("Mapped found series: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<SeriesDto> updateSeries(@RequestBody SeriesDto seriesDto) {
        log.info("Request to update series received with data: {}", seriesDto);
        Series updateSeries = mapper.map(seriesDto, Series.class);
        log.debug("Mapped Series DTO to domain object: {}", updateSeries);
        Series updatedSeries = saveSeriesUseCase.updateSeries(updateSeries);
        log.info("Series updated successfully: {}", updatedSeries);
        SeriesDto dto = mapper.map(updatedSeries, SeriesDto.class);
        log.debug("Mapped updated series to DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<SeriesDto> addAuthor(@RequestBody SeriesDto seriesDto) {
        log.info("Request to add seriesDto: {}", seriesDto);

        Series seriesToAdd =  mapper.map(seriesDto, Series.class);
        log.debug("Mapped Series DTO to domain object: {}", seriesToAdd);

        Series added = saveSeriesUseCase.addSeries(seriesToAdd);
        log.info("Added seriesDto: {}", added);

        SeriesDto dto = mapper.map(added, SeriesDto.class);
        log.debug("Mapped domain object to Series DTO: {}", added);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);

    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<SeriesDto> getCategoryByName(@RequestParam String title) {
        log.info("Request to search series by title: {}", title);
        Series series = findSeriesUseCase.findSeriesByTitle(title);

        if (series == null) {
            log.warn("No series found with title: {}", title);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found series matching name: {}", title);
        SeriesDto dto = mapper.map(series, SeriesDto.class);
        log.debug("Mapped domain object to Series DTO: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}