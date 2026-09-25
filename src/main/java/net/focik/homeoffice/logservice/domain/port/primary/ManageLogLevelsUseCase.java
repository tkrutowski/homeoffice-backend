package net.focik.homeoffice.logservice.domain.port.primary;

import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LogLevelOverride;
import net.focik.homeoffice.logservice.domain.model.LogLevelsInfo;

public interface ManageLogLevelsUseCase {
    LogLevelsInfo getLevels();

    /**
     * Tymczasowo ustawia poziom loggera; po uplywie TTL wraca poprzedni poziom. Ponowne wywolanie dla tego samego
     * loggera zastepuje poziom i przedluza TTL, ale wraca sie nadal do poziomu sprzed pierwszej zmiany.
     *
     * @param ttlMinutes czas trwania zmiany, {@code null} = wartosc domyslna
     * @param changedBy  uzytkownik wykonujacy zmiane (do logu audytowego)
     */
    LogLevelOverride setLevel(String logger, LogLevel level, Integer ttlMinutes, String changedBy);

    /**
     * Natychmiast przywraca poziom sprzed nadpisania.
     */
    void resetLevel(String logger, String changedBy);
}
