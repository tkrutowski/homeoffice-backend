package net.focik.homeoffice.finance.api;

import net.focik.homeoffice.finance.domain.payment.PaymentReminderService;
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
    void shouldTriggerPaymentReminderProcessing() {
        // When
        ResponseEntity<String> response = paymentReminderTestController.triggerPaymentReminderProcessing();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(paymentReminderService, times(1)).processPaymentReminders();
    }

    @Test
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
    void shouldSendTestReminder() {
        // When
        ResponseEntity<String> response = paymentReminderTestController.sendTestReminder(1, "FEE", 1, 1);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(paymentReminderService, times(1)).sendPaymentReminderForInstallment(1, "FEE", 1, 1);
    }

    @Test
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
    void shouldSendTestReminderForLoan() {
        // When
        ResponseEntity<String> response = paymentReminderTestController.sendTestReminder(1, "LOAN", 1, 1);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(paymentReminderService, times(1)).sendPaymentReminderForInstallment(1, "LOAN", 1, 1);
    }
}
