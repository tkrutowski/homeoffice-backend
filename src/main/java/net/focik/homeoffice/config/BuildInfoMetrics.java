package net.focik.homeoffice.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/**
 * Metryka "info": {@code homeoffice_build_info{version="x.y.z"} 1}. Pozwala w Grafanie zobaczyc, jaka wersja
 * dziala na ktorej instancji (tag {@code instance} dodaje konfiguracja metryk). Wersja jest tagiem tylko tej
 * jednej metryki, a nie wszystkich - kazda nowa wartosc taga tworzy nowa serie.
 * <p>
 * {@link BuildProperties} istnieje tylko po buildzie Mavena (cel {@code build-info}); przy uruchomieniu z IDE
 * bez tego pliku wersja to {@code unknown}.
 */
@Component
public class BuildInfoMetrics implements MeterBinder {

    private final ObjectProvider<BuildProperties> buildProperties;

    public BuildInfoMetrics(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        BuildProperties properties = buildProperties.getIfAvailable();
        String version = properties != null && properties.getVersion() != null ? properties.getVersion() : "unknown";

        Gauge.builder("homeoffice.build.info", () -> 1)
                .description("Wersja aplikacji (tag version); wartosc zawsze 1")
                .tag("version", version)
                .register(registry);
    }
}
