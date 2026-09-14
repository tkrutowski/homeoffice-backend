package net.focik.homeoffice.finance.api;

import net.focik.homeoffice.finance.domain.payment.PaymentReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentReminderTestControllerTest {

    @Mock
    private PaymentReminderService paymentReminderService;

    @InjectMocks
    private PaymentReminderTestController paymentReminderTestController;

    @Test
    @DisplayName("should trigger payment reminder processing and return 200 OK")
    void shouldTriggerPaymentReminderProcessing() {
        // When
        ResponseEntity<String> response = paymentReminderTestController.triggerPaymentReminderProcessing();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(paymentReminderService, times(1)).processPaymentReminders();
    }

    @Test
    @DisplayName("should return 500 with an error message when processing reminders throws")
    void shouldHandleErrorWhenProcessingReminders() {
        // Given
        doThrow(new RuntimeException("Email service error")).when(paymentReminderService).processPaymentReminders();

        // When
        ResponseEntity<String> response = paymentReminderTestController.triggerPaymentReminderProcessing();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error"));
        verify(paymentReminderService, times(1)).processPaymentReminders();
    }

    @Test
    @DisplayName("should send a test fee reminder and return 200 OK")
    void shouldSendTestReminder() {
        // When
        ResponseEntity<String> response = paymentReminderTestController.sendTestReminder(1, "FEE", 1, 1);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(paymentReminderService, times(1)).sendPaymentReminderForInstallment(1, "FEE", 1, 1);
    }

    @Test
    @DisplayName("should return 500 with an error message when sending a test reminder throws")
    void shouldHandleErrorWhenSendingTestReminder() {
        // Given
        doThrow(new RuntimeException("User not found")).when(paymentReminderService)
                .sendPaymentReminderForInstallment(any(), any(), any(), any());

        // When
        ResponseEntity<String> response = paymentReminderTestController.sendTestReminder(999, "FEE", 999, 999);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error"));
        verify(paymentReminderService, times(1)).sendPaymentReminderForInstallment(999, "FEE", 999, 999);
    }

    @Test
    @DisplayName("should send a test loan reminder and return 200 OK")
    void shouldSendTestReminderForLoan() {
        // When
        ResponseEntity<String> response = paymentReminderTestController.sendTestReminder(1, "LOAN", 1, 1);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(paymentReminderService, times(1)).sendPaymentReminderForInstallment(1, "LOAN", 1, 1);
    }
}
