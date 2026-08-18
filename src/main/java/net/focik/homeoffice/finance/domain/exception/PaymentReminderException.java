package net.focik.homeoffice.finance.domain.exception;

/**
 * Exception thrown when payment reminder processing fails
 */
public class PaymentReminderException extends RuntimeException {

    public PaymentReminderException(String message) {
        super(message);
    }

    public PaymentReminderException(String message, Throwable cause) {
        super(message, cause);
    }
}
