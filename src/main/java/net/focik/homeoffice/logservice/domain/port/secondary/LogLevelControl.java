package net.focik.homeoffice.logservice.domain.port.secondary;

import net.focik.homeoffice.logservice.domain.model.LogLevel;

/**
 * Dostep do poziomow loggerow dzialajacej aplikacji.
 */
public interface LogLevelControl {
    /**
     * @return poziom skonfigurowany wprost dla loggera albo {@code null}, gdy dziedziczy po nadrzednym
     */
    LogLevel getConfiguredLevel(String logger);

    /**
     * @return efektywny poziom root loggera
     */
    LogLevel getRootLevel();

    /**
     * @param level nowy poziom; {@code null} = usuniecie konfiguracji (logger dziedziczy po nadrzednym)
     */
    void setLevel(String logger, LogLevel level);
}
