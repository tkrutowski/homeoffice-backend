package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.Series;

public interface SaveSeriesUseCase {
    Series addSeries(Series series);

    Series updateSeries(Series series);
}
