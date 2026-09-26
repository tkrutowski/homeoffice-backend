package net.focik.homeoffice.logservice.domain;

import net.focik.homeoffice.logservice.domain.model.LogLevelsInfo;
import net.focik.homeoffice.logservice.domain.port.primary.ManageLogLevelsUseCase;
import net.focik.homeoffice.logservice.domain.port.secondary.LogLevelControl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

/**
 * Sprawdza, ze bean wiaze sie w kontekscie Springa (wybor konstruktora, wstrzykniecie wartosci z properties).
 */
class LogLevelsServiceWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(LogLevelControl.class, () -> mock(LogLevelControl.class))
            .withUserConfiguration(LogLevelsService.class);

    @Test
    @DisplayName("should use the default prefixes and TTLs when nothing is configured")
    void usesDefaultsWhenNothingConfigured() {
        runner.run(context -> {
            LogLevelsInfo info = context.getBean(ManageLogLevelsUseCase.class).getLevels();

            assertEquals(List.of("net.focik.homeoffice", "org.springframework.security", "org.hibernate.SQL", "software.amazon.awssdk"),
                    info.allowedLoggers());
            assertEquals(15, info.defaultTtlMinutes());
            assertEquals(1440, info.maxTtlMinutes());
            assertEquals("unknown", info.instance());
            assertNull(info.rootLevel());
        });
    }

    @Test
    @DisplayName("should read the configured prefixes and TTLs")
    void readsConfiguredPrefixesAndTtls() {
        runner.withPropertyValues(
                        "logs.levels.allowed-prefixes=com.example, org.foo.Bar",
                        "logs.levels.default-ttl-minutes=5",
                        "logs.levels.max-ttl-minutes=60",
                        "APP_INSTANCE=synology")
                .run(context -> {
                    LogLevelsInfo info = context.getBean(ManageLogLevelsUseCase.class).getLevels();

                    assertEquals(List.of("com.example", "org.foo.Bar"), info.allowedLoggers());
                    assertEquals(5, info.defaultTtlMinutes());
                    assertEquals(60, info.maxTtlMinutes());
                    assertEquals("synology", info.instance());
                });
    }
}
