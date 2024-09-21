package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
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

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/library/series")
public class SeriesController {

    private final FindSeriesUseCase findSeriesUseCase;
    private final SaveSeriesUseCase saveSeriesUseCase;
    private final ModelMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ')")
    ResponseEntity<List<SeriesDto>> getAllSeries() {
        List<Series> allSeries = findSeriesUseCase.getAllSeries();
        return new ResponseEntity<>(allSeries.stream()
                .map(series -> mapper.map(series, SeriesDto.class))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ')")
    ResponseEntity<SeriesDto> getSeries(@PathVariable Integer id) {
        Series series = findSeriesUseCase.findSeries(id);
        return new ResponseEntity<>(mapper.map(series, SeriesDto.class), HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('LIBRARY_WRITE_ALL','LIBRARY_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<SeriesDto> updateSeries(@RequestBody SeriesDto seriesDto) {
        Series updateSeries = mapper.map(seriesDto, Series.class);
        Series updatedSeries = saveSeriesUseCase.updateSeries(updateSeries);
        return new ResponseEntity<>(mapper.map(updatedSeries, SeriesDto.class), HttpStatus.OK);
    }
}