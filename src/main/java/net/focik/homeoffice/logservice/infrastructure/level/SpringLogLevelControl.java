package net.focik.homeoffice.logservice.infrastructure.level;

import net.focik.homeoffice.logservice.domain.model.LogLevel;
import net.focik.homeoffice.logservice.domain.model.LoggerInfo;
import net.focik.homeoffice.logservice.domain.port.secondary.LogLevelControl;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Zmienia poziomy loggerow przez {@link LoggingSystem} Spring Boota (na produkcji: logback).
 * Poziomy spoza {@link LogLevel} (FATAL, OFF) sa traktowane jak brak wprost skonfigurowanego poziomu.
 */
@Component
public class SpringLogLevelControl implements LogLevelControl {

    private final LoggingSystem loggingSystem;

    public SpringLogLevelControl(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    @Override
    public LogLevel getConfiguredLevel(String logger) {
        LoggerConfiguration configuration = loggingSystem.getLoggerConfiguration(logger);
        return configuration == null ? null : toDomain(configuration.getConfiguredLevel());
    }

    @Override
    public LogLevel getRootLevel() {
        LoggerConfiguration configuration = loggingSystem.getLoggerConfiguration(LoggingSystem.ROOT_LOGGER_NAME);
        return configuration == null ? null : toDomain(configuration.getEffectiveLevel());
    }

    @Override
    public List<LoggerInfo> getLoggers() {
        return loggingSystem.getLoggerConfigurations().stream()
                .map(configuration -> new LoggerInfo(configuration.getName(),
                        toDomain(configuration.getConfiguredLevel()), toDomain(configuration.getEffectiveLevel())))
                .toList();
    }

    @Override
    public void setLevel(String logger, LogLevel level) {
        loggingSystem.setLogLevel(logger, level == null ? null : org.springframework.boot.logging.LogLevel.valueOf(level.name()));
    }

    private static LogLevel toDomain(org.springframework.boot.logging.LogLevel level) {
        if (level == null) {
            return null;
        }
        try {
            return LogLevel.valueOf(level.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
