package net.focik.homeoffice.logservice.api.dto;

import net.focik.homeoffice.logservice.domain.model.LogLevel;

/**
 * @param logger     nazwa loggera / pakietu (musi zaczynac sie od jednego z dozwolonych prefiksow)
 * @param level      nowy poziom
 * @param ttlMinutes czas trwania zmiany w minutach, {@code null} = wartosc domyslna
 */
public record SetLogLevelRequest(String logger, LogLevel level, Integer ttlMinutes) {
}
