package net.focik.homeoffice.logservice.domain.model;

/**
 * Logger (pakiet albo klasa) istniejacy w dzialajacej aplikacji.
 *
 * @param name            pelna nazwa loggera
 * @param configuredLevel poziom skonfigurowany wprost ({@code null} = dziedziczony po nadrzednym)
 * @param effectiveLevel  poziom faktycznie obowiazujacy ({@code null}, gdy nie da sie go zmapowac na {@link LogLevel})
 */
public record LoggerInfo(String name, LogLevel configuredLevel, LogLevel effectiveLevel) {
}
