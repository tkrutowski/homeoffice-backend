package net.focik.homeoffice.logservice.domain.model;

import java.time.LocalDateTime;

/**
 * Tymczasowa zmiana poziomu loggera na biezacej instancji.
 *
 * @param logger        nazwa loggera / pakietu
 * @param level         ustawiony poziom
 * @param previousLevel poziom skonfigurowany przed pierwsza zmiana ({@code null} = dziedziczony po nadrzednym);
 *                      do niego aplikacja wraca po uplywie TTL
 * @param setAt         moment ustawienia
 * @param revertsAt     moment automatycznego powrotu do {@code previousLevel}
 * @param changedBy     uzytkownik, ktory wykonal zmiane
 */
public record LogLevelOverride(String logger, LogLevel level, LogLevel previousLevel,
                               LocalDateTime setAt, LocalDateTime revertsAt, String changedBy) {
}
