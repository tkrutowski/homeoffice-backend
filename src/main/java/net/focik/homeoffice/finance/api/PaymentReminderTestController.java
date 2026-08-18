package net.focik.homeoffice.finance.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.payment.PaymentReminderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for testing payment reminders
 * Allows on-demand sending of payment reminder emails
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/finance/payment-reminders")
@RequiredArgsConstructor
public class PaymentReminderTestController {

    private final PaymentReminderService paymentReminderService;

    /**
     * Trigger payment reminder processing immediately
     * Used for testing the scheduler logic without waiting for scheduled time
     *
     * @return success message
     */
    @PostMapping("/process")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> triggerPaymentReminderProcessing() {
        try {
            log.info("Manual trigger of payment reminder processing");
            paymentReminderService.processPaymentReminders();
            return ResponseEntity.ok("Payment reminders processed successfully");
        } catch (Exception e) {
            log.error("Error processing payment reminders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment reminders: " + e.getMessage());
        }
    }

    /**
     * Send a payment reminder email for a specific installment
     * Useful for testing the email template and logic
     *
     * @param userId user ID
     * @param paymentType type of payment (FEE or LOAN)
     * @param paymentId ID of Fee or Loan
     * @param installmentId ID of the installment
     * @return success message
     */
    @PostMapping("/test")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> sendTestReminder(
            @RequestParam Integer userId,
            @RequestParam String paymentType,
            @RequestParam Integer paymentId,
            @RequestParam Integer installmentId) {
        try {
            log.info("Manual trigger of payment reminder for userId: {}, paymentType: {}, paymentId: {}, installmentId: {}",
                    userId, paymentType, paymentId, installmentId);
            paymentReminderService.sendPaymentReminderForInstallment(userId, paymentType, paymentId, installmentId);
            return ResponseEntity.ok("Payment reminder sent successfully");
        } catch (Exception e) {
            log.error("Error sending payment reminder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending payment reminder: " + e.getMessage());
        }
    }
}
