package net.focik.homeoffice.logservice.infrastructure.level;

import net.focik.homeoffice.logservice.domain.model.LogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggingSystem;

import static org.junit.jupiter.api.Assertions.*;

class SpringLogLevelControlTest {

    private static final String LOGGER = "leveltest.sample.Package";

    private final SpringLogLevelControl control =
            new SpringLogLevelControl(LoggingSystem.get(getClass().getClassLoader()));

    @AfterEach
    void cleanUp() {
        control.setLevel(LOGGER, null);
    }

    @Test
    void setLevelChangesRealLoggerAndNullRestoresInheritance() {
        assertNull(control.getConfiguredLevel(LOGGER));

        control.setLevel(LOGGER, LogLevel.DEBUG);

        assertEquals(LogLevel.DEBUG, control.getConfiguredLevel(LOGGER));
        assertTrue(LoggerFactory.getLogger(LOGGER).isDebugEnabled());

        control.setLevel(LOGGER, LogLevel.ERROR);
        assertEquals(LogLevel.ERROR, control.getConfiguredLevel(LOGGER));
        assertFalse(LoggerFactory.getLogger(LOGGER).isWarnEnabled());

        control.setLevel(LOGGER, null);
        assertNull(control.getConfiguredLevel(LOGGER));
    }

    @Test
    void rootLevelIsAvailable() {
        assertNotNull(control.getRootLevel());
    }
}
