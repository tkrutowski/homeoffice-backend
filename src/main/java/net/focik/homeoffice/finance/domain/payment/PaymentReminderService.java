package net.focik.homeoffice.finance.domain.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.finance.domain.exception.PaymentReminderException;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.fee.port.primary.GetFeeUseCase;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.domain.loan.port.primary.GetLoanUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.MoneyUtils;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service for managing payment reminders
 * Handles business logic for sending payment reminders:
 * - 7, 3, 1 days before payment deadline
 * - 1, 3, 7 days after payment deadline (overdue)
 * - Weekly after that (every 7 days), until the installment is paid
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReminderService {

    private final GetFeeUseCase getFeeUseCase;
    private final GetLoanUseCase getLoanUseCase;
    private final EmailNotificationPort emailNotificationPort;
    private final UserFacade userFacade;

    @Value("${app.payment-reminder.enabled:true}")
    private boolean reminderEnabled;

    /**
     * Scheduled task to process payment reminders
     * Runs daily at the configured CRON schedule (default: 9:00 AM)
     */
    @Scheduled(cron = "${app.payment-reminder.cron:0 0 9 * * *}")
    public void scheduledProcessPaymentReminders() {
        if (!reminderEnabled) {
            log.debug("Payment reminders are disabled");
            return;
        }

        try {
            log.debug("Triggering scheduled payment reminder processing");
            processPaymentReminders();
        } catch (Exception e) {
            log.error("Error in scheduled payment reminder processing", e);
        }
    }

    /**
     * Process and send payment reminders for all users
     * Iterates through all non-paid fees and loans, checks deadlines,
     * and sends reminders accordingly
     */
    public void processPaymentReminders() {
        log.info("Starting payment reminder processing");
        LocalDate today = LocalDate.now();

        try {
            // Process Fee reminders
            processFeeReminders(today);

            // Process Loan reminders
            processLoanReminders(today);

            log.info("Payment reminder processing completed successfully");
        } catch (Exception e) {
            log.error("Error during payment reminder processing", e);
        }
    }

    /**
     * Send a payment reminder to a specific user for a specific installment
     *
     * @param userId user ID
     * @param paymentType type of payment (FEE or LOAN)
     * @param paymentId ID of Fee or Loan
     * @param installmentId ID of the installment
     */
    public void sendPaymentReminderForInstallment(Integer userId, String paymentType, Integer paymentId, Integer installmentId) {
        AppUser user = userFacade.findUserById(Long.valueOf(userId));

        if (user == null || user.getEmail() == null) {
            log.warn("User not found or has no email for user ID: {}", userId);
            throw new PaymentReminderException("User not found or has no email for user ID: " + userId);
        }

        if ("FEE".equalsIgnoreCase(paymentType)) {
            sendFeeReminderById(paymentId, installmentId);
        } else if ("LOAN".equalsIgnoreCase(paymentType)) {
            sendLoanReminderById(paymentId, installmentId);
        } else {
            log.warn("Unknown payment type: {}", paymentType);
            throw new PaymentReminderException("Unknown payment type: " + paymentType);
        }
    }

    /**
     * Process reminders for all fees
     */
    private void processFeeReminders(LocalDate today) {
        try {
            List<Fee> allFees = getFeeUseCase.getFeesByStatus(PaymentStatus.TO_PAY, true);
            allFees
                    .forEach(fee -> processFeeInstallments(fee, today));
        } catch (Exception e) {
            log.error("Error processing fee reminders", e);
        }
    }

    /**
     * Process reminders for all loans
     */
    private void processLoanReminders(LocalDate today) {
        try {
            List<Loan> allLoans = getLoanUseCase.getLoansByStatus(PaymentStatus.TO_PAY, true);
            allLoans
                    .forEach(loan -> processLoanInstallments(loan, today));
        } catch (Exception e) {
            log.error("Error processing loan reminders", e);
        }
    }

    /**
     * Process installments for a specific fee
     */
    private void processFeeInstallments(Fee fee, LocalDate today) {
        if (fee.getInstallments() == null || fee.getInstallments().isEmpty()) {
            return;
        }

        fee.getInstallments().stream()
                .filter(installment -> installment.getPaymentStatus() != PaymentStatus.PAID)
                .filter(installment -> shouldSendReminder(installment.getPaymentDeadline(), today))
                .forEach(installment -> {
                    try {
                        sendFeeReminderEmail(fee, installment);
                        log.debug("Fee reminder sent for fee ID: {}, installment ID: {}",
                                fee.getId(), installment.getIdFeeInstallment());
                    } catch (Exception e) {
                        log.error("Error sending fee reminder for fee ID: {}, installment ID: {}",
                                fee.getId(), installment.getIdFeeInstallment(), e);
                    }
                });
    }

    /**
     * Process installments for a specific loan
     */
    private void processLoanInstallments(Loan loan, LocalDate today) {
        if (loan.getInstallments() == null || loan.getInstallments().isEmpty()) {
            return;
        }

        loan.getInstallments().stream()
                .filter(installment -> installment.getPaymentStatus() != PaymentStatus.PAID)
                .filter(installment -> shouldSendReminder(installment.getPaymentDeadline(), today))
                .forEach(installment -> {
                    try {
                        sendLoanReminderEmail(loan, installment);
                        log.debug("Loan reminder sent for loan ID: {}, installment ID: {}",
                                loan.getId(), installment.getIdLoanInstallment());
                    } catch (Exception e) {
                        log.error("Error sending loan reminder for loan ID: {}, installment ID: {}",
                                loan.getId(), installment.getIdLoanInstallment(), e);
                    }
                });
    }

    /**
     * Determine if a reminder should be sent based on the deadline date
     * Sends reminders:
     * - 7, 3, or 1 days before deadline (if deadline is in the future)
     * - 1, 3, or 7 days after deadline (overdue)
     * - Every 7 days after that (14, 21, 28, ...), until the installment is paid
     *
     * @param deadlineDate the payment deadline
     * @param today today's date
     * @return true if reminder should be sent
     */
    private boolean shouldSendReminder(LocalDate deadlineDate, LocalDate today) {
        long daysUntilDeadline = ChronoUnit.DAYS.between(today, deadlineDate);
        if (daysUntilDeadline == 7L || daysUntilDeadline == 3L || daysUntilDeadline == 1L) {
            return true;
        }

        long daysAfterDeadline = ChronoUnit.DAYS.between(deadlineDate, today);
        if (daysAfterDeadline == 1L || daysAfterDeadline == 3L || daysAfterDeadline == 7L) {
            return true;
        }

        // After the first week overdue, remind weekly
        return daysAfterDeadline > 7L && daysAfterDeadline % 7L == 0L;
    }

    /**
     * Send reminder email for a fee installment
     */
    private void sendFeeReminderEmail(Fee fee, FeeInstallment installment) {
        AppUser user = userFacade.findUserById(Long.valueOf(fee.getIdUser()));
        if (user == null || user.getEmail() == null) {
            log.warn("User not found or has no email for fee ID: {}", fee.getId());
            return;
        }

        LocalDate today = LocalDate.now();
        long daysUntilDeadline = ChronoUnit.DAYS.between(today, installment.getPaymentDeadline());

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        templateVariables.put("description", fee.getName());
        templateVariables.put("amount", MoneyUtils.mapMoneyToString(installment.getInstallmentAmountToPay()));
        templateVariables.put("dueDate", installment.getPaymentDeadline());
        templateVariables.put("daysUntilDeadline", daysUntilDeadline);
        templateVariables.put("currentYear", Year.now().getValue());

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(user.getEmail());
        emailRequest.setSubject("Przypomnienie o płatności: " + fee.getName());
        emailRequest.setTemplateName("payment-reminder.html");
        emailRequest.setTemplateVariables(templateVariables);

        emailNotificationPort.sendTemplatedEmail(emailRequest);
    }

    /**
     * Send reminder email for a loan installment
     */
    private void sendLoanReminderEmail(Loan loan, LoanInstallment installment) {
        AppUser user = userFacade.findUserById((long) loan.getIdUser());
        if (user == null || user.getEmail() == null) {
            log.warn("User not found or has no email for loan ID: {}", loan.getId());
            return;
        }

        LocalDate today = LocalDate.now();
        long daysUntilDeadline = ChronoUnit.DAYS.between(today, installment.getPaymentDeadline());

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        templateVariables.put("description", loan.getName());
        templateVariables.put("amount", MoneyUtils.mapMoneyToString(installment.getInstallmentAmountToPay()));
        templateVariables.put("dueDate", installment.getPaymentDeadline());
        templateVariables.put("daysUntilDeadline", daysUntilDeadline);
        templateVariables.put("currentYear", Year.now().getValue());

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(user.getEmail());
        emailRequest.setSubject("Przypomnienie o płatności: " + loan.getName());
        emailRequest.setTemplateName("payment-reminder.html");
        emailRequest.setTemplateVariables(templateVariables);

        emailNotificationPort.sendTemplatedEmail(emailRequest);
    }

    /**
     * Send reminder email for a specific fee installment
     */
    private void sendFeeReminderById(Integer feeId, Integer installmentId) {
        var fee = getFeeUseCase.getFeeById(feeId, true);

        var installment = fee.getInstallments().stream()
                .filter(inst -> inst.getIdFeeInstallment().equals(installmentId))
                .findFirst();

        if (installment.isPresent()) {
            sendFeeReminderEmail(fee, installment.get());
        } else {
            log.warn("Fee installment not found with ID: {}", installmentId);
            throw new PaymentReminderException("Fee installment not found with ID: " + installmentId);
        }
    }

    /**
     * Send reminder email for a specific loan installment
     */
    private void sendLoanReminderById(Integer loanId, Integer installmentId) {
        var loan = getLoanUseCase.getLoanById(loanId, true);
        if (loan == null) {
            log.warn("Loan not found with ID: {}", loanId);
            throw new PaymentReminderException("Loan not found with ID: " + loanId);
        }

        var installment = loan.getInstallments().stream()
                .filter(inst -> inst.getIdLoanInstallment() == installmentId)
                .findFirst();

        if (installment.isPresent()) {
            sendLoanReminderEmail(loan, installment.get());
        } else {
            log.warn("Loan installment not found with ID: {}", installmentId);
            throw new PaymentReminderException("Loan installment not found with ID: " + installmentId);
        }
    }
}
