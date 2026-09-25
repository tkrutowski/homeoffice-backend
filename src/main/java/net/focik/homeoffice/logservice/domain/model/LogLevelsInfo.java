package net.focik.homeoffice.logservice.domain.model;

import java.util.List;

/**
 * Stan zarzadzania poziomami logow na biezacej instancji.
 *
 * @param instance          nazwa biezacej instancji
 * @param rootLevel         aktualny poziom root loggera (tylko do odczytu)
 * @param allowedLoggers    prefiksy loggerow, ktorych poziom mozna zmieniac (nazwa rowna prefiksowi albo zaczynajaca
 *                          sie od {@code prefiks.})
 * @param defaultTtlMinutes TTL uzywany, gdy zadanie go nie podaje
 * @param maxTtlMinutes     maksymalny dozwolony TTL
 * @param overrides         aktywne nadpisania
 */
public record LogLevelsInfo(String instance, LogLevel rootLevel, List<String> allowedLoggers,
                            int defaultTtlMinutes, int maxTtlMinutes, List<LogLevelOverride> overrides) {
}
