package net.focik.homeoffice.goahead.domain.exception;

public class KsefResponseException extends RuntimeException {
    /** Kod statusu procesu uwierzytelniania zwrocony przez KSeF (>= 400), jesli wyjatek go dotyczy. */
    private final Integer authStatusCode;

    public KsefResponseException(String message) {
        super(message);
        this.authStatusCode = null;
    }
    public KsefResponseException(String message, Throwable cause) {
        super(message, cause);
        this.authStatusCode = null;
    }
    public KsefResponseException(String message, int authStatusCode) {
        super(message);
        this.authStatusCode = authStatusCode;
    }

    public Integer getAuthStatusCode() {
        return authStatusCode;
    }
}
