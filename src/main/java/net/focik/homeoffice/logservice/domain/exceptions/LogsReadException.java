package net.focik.homeoffice.logservice.domain.exceptions;

public class LogsReadException extends RuntimeException {
    public LogsReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
