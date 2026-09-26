package net.focik.homeoffice.logservice.domain.port.secondary;

import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LoggerInfo;

import java.util.List;

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
     * @return wszystkie loggery, ktore istnieja w tej chwili w aplikacji (tylko te, ktore juz zostaly utworzone)
     */
    List<LoggerInfo> getLoggers();

    /**
     * @param level nowy poziom; {@code null} = usuniecie konfiguracji (logger dziedziczy po nadrzednym)
     */
    void setLevel(String logger, LogLevel level);
}
