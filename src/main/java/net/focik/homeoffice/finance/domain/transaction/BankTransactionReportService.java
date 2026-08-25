package net.focik.homeoffice.finance.domain.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GenerateBankTransactionReportUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GetBankTransactionUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.MoneyUtils;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain service for managing bank transaction reports (payments from account)
 * Handles business logic for sending monthly and weekly transaction summaries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransactionReportService implements GenerateBankTransactionReportUseCase {

    private final GetBankTransactionUseCase getBankTransactionUseCase;
    private final EmailNotificationPort emailNotificationPort;
    private final UserFacade userFacade;

    @Value("${app.transaction-report.enabled:true}")
    private boolean transactionReportEnabled;

    /**
     * Scheduled task to process monthly bank transaction reports
     * Runs on the 1st day of each month at 10:00 AM
     */
    @Scheduled(cron = "${app.transaction-report.monthly.cron:0 0 10 1 * *}")
    public void scheduledProcessMonthlyReports() {
        if (!transactionReportEnabled) {
            log.debug("Bank transaction reports are disabled");
            return;
        }

        try {
            log.debug("Triggering scheduled monthly bank transaction report processing");
            processMonthlyReports();
        } catch (Exception e) {
            log.error("Error in scheduled monthly bank transaction report processing", e);
        }
    }

    /**
     * Scheduled task to process weekly bank transaction reports
     * Runs every Monday at 9:30 AM
     */
    @Scheduled(cron = "${app.transaction-report.weekly.cron:0 30 9 ? * MON}")
    public void scheduledProcessWeeklyReports() {
        if (!transactionReportEnabled) {
            log.debug("Bank transaction reports are disabled");
            return;
        }

        try {
            log.debug("Triggering scheduled weekly bank transaction report processing");
            processWeeklyReports();
        } catch (Exception e) {
            log.error("Error in scheduled weekly bank transaction report processing", e);
        }
    }

    /**
     * Process and send monthly bank transaction reports for all users
     */
    public void processMonthlyReports() {
        log.info("Starting monthly bank transaction report processing");

        try {
            LocalDate today = LocalDate.now();
            LocalDate firstDayOfPreviousMonth = today.minusMonths(1)
                    .with(TemporalAdjusters.firstDayOfMonth());
            LocalDate lastDayOfPreviousMonth = today.minusMonths(1)
                    .with(TemporalAdjusters.lastDayOfMonth());

            List<AppUser> allUsers = userFacade.getAllUsers();
            for (AppUser user : allUsers) {
                try {
                    List<BankTransaction> transactions = getBankTransactionUseCase.findBetween(
                            firstDayOfPreviousMonth,
                            lastDayOfPreviousMonth,
                            Math.toIntExact(user.getId())
                    );

                    if (!transactions.isEmpty()) {
                        sendMonthlyReportEmail(user, transactions, firstDayOfPreviousMonth);
                    }
                } catch (Exception e) {
                    log.error("Error processing monthly report for user: {}", user.getUsername(), e);
                }
            }

            log.info("Monthly bank transaction report processing completed successfully");
        } catch (Exception e) {
            log.error("Error during monthly bank transaction report processing", e);
        }
    }

    /**
     * Process and send weekly bank transaction reports for all users
     */
    public void processWeeklyReports() {
        log.info("Starting weekly bank transaction report processing");

        try {
            LocalDate today = LocalDate.now();
            LocalDate lastSunday = today.minusDays(today.getDayOfWeek().getValue() % 7);
            LocalDate previousMonday = lastSunday.minusDays(6);

            List<AppUser> allUsers = userFacade.getAllUsers();
            for (AppUser user : allUsers) {
                try {
                    List<BankTransaction> transactions = getBankTransactionUseCase.findBetween(
                            previousMonday,
                            lastSunday,
                            Math.toIntExact(user.getId())
                    );

                    if (!transactions.isEmpty()) {
                        sendWeeklyReportEmail(user, transactions, previousMonday, lastSunday);
                    }
                } catch (Exception e) {
                    log.error("Error processing weekly report for user: {}", user.getUsername(), e);
                }
            }

            log.info("Weekly bank transaction report processing completed successfully");
        } catch (Exception e) {
            log.error("Error during weekly bank transaction report processing", e);
        }
    }

    /**
     * Send monthly bank transaction report email
     */
    private void sendMonthlyReportEmail(AppUser user, List<BankTransaction> transactions, LocalDate monthDate) {
        if (user == null || user.getEmail() == null) {
            log.warn("User not found or has no email");
            return;
        }

        YearMonth month = YearMonth.from(monthDate);
        Map<String, Object> templateVariables = prepareTemplateVariables(
                user,
                transactions,
                month.toString()
        );

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(user.getEmail());
        emailRequest.setSubject("Miesięczne podsumowanie transakcji - " + month.toString());
        emailRequest.setTemplateName("expense-report.html");
        emailRequest.setTemplateVariables(templateVariables);

        emailNotificationPort.sendTemplatedEmail(emailRequest);
    }

    /**
     * Send weekly bank transaction report email
     */
    private void sendWeeklyReportEmail(AppUser user, List<BankTransaction> transactions, LocalDate weekStart, LocalDate weekEnd) {
        if (user == null || user.getEmail() == null) {
            log.warn("User not found or has no email");
            return;
        }

        String weekLabel = String.format("tydzień %d-%d.%d",
                weekStart.getDayOfMonth(),
                weekEnd.getDayOfMonth(),
                weekEnd.getMonthValue()
        );

        Map<String, Object> templateVariables = prepareTemplateVariables(
                user,
                transactions,
                weekLabel
        );

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(user.getEmail());
        emailRequest.setSubject("Tygodniowe podsumowanie transakcji - " + weekLabel);
        emailRequest.setTemplateName("expense-report.html");
        emailRequest.setTemplateVariables(templateVariables);

        emailNotificationPort.sendTemplatedEmail(emailRequest);
    }

    /**
     * Prepare template variables for bank transaction report email
     */
    private Map<String, Object> prepareTemplateVariables(
            AppUser user,
            List<BankTransaction> transactions,
            String period) {

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        templateVariables.put("month", "Transakcje z konta - " + period);

        // Separate income from expenses
        List<BankTransaction> incomeTransactions = transactions.stream()
                .filter(t -> t.getTransactionType() == net.focik.homeoffice.finance.domain.transaction.model.TransactionType.TRANSFER_IN ||
                             t.getTransactionType() == net.focik.homeoffice.finance.domain.transaction.model.TransactionType.DEPOSIT)
                .toList();

        List<BankTransaction> expenseTransactions = transactions.stream()
                .filter(t -> t.getTransactionType() != net.focik.homeoffice.finance.domain.transaction.model.TransactionType.TRANSFER_IN &&
                             t.getTransactionType() != net.focik.homeoffice.finance.domain.transaction.model.TransactionType.DEPOSIT)
                .toList();

        // Calculate totals for expenses
        Money expenseTotal = expenseTransactions.stream()
                .map(t -> Money.of(t.getAmount(), "PLN"))
                .reduce(Money.of(0, "PLN"), Money::add);

        templateVariables.put("totalAmount", MoneyUtils.mapMoneyToString(expenseTotal));

        // Calculate average for expenses
        if (!expenseTransactions.isEmpty()) {
            Money averageAmount = Money.of(
                    expenseTotal.getNumber().doubleValue() / expenseTransactions.size(),
                    "PLN"
            );
            templateVariables.put("averageAmount", MoneyUtils.mapMoneyToString(averageAmount));
        } else {
            templateVariables.put("averageAmount", "0,00 zł");
        }

        // Calculate totals for income
        Money incomeTotal = incomeTransactions.stream()
                .map(t -> Money.of(t.getAmount(), "PLN"))
                .reduce(Money.of(0, "PLN"), Money::add);

        templateVariables.put("totalIncome", MoneyUtils.mapMoneyToString(incomeTotal));

        // Calculate average for income
        if (!incomeTransactions.isEmpty()) {
            Money averageIncome = Money.of(
                    incomeTotal.getNumber().doubleValue() / incomeTransactions.size(),
                    "PLN"
            );
            templateVariables.put("averageIncome", MoneyUtils.mapMoneyToString(averageIncome));
        } else {
            templateVariables.put("averageIncome", "0,00 zł");
        }

        // Format expenses for display
        var formattedExpenses = expenseTransactions.stream()
                .map(transaction -> {
                    Map<String, Object> txMap = new HashMap<>();
                    txMap.put("description", transaction.getDescription() + " (" + transaction.getTransactionType().getTranslate() + ")");
                    txMap.put("entryDate", transaction.getTransactionDate());
                    txMap.put("amount", MoneyUtils.mapMoneyToString(Money.of(transaction.getAmount(), "PLN")));
                    return txMap;
                })
                .collect(Collectors.toList());

        // Format income for display
        var formattedIncome = incomeTransactions.stream()
                .map(transaction -> {
                    Map<String, Object> txMap = new HashMap<>();
                    txMap.put("description", transaction.getDescription() + " (" + transaction.getTransactionType().getTranslate() + ")");
                    txMap.put("entryDate", transaction.getTransactionDate());
                    txMap.put("amount", MoneyUtils.mapMoneyToString(Money.of(transaction.getAmount(), "PLN")));
                    return txMap;
                })
                .collect(Collectors.toList());

        templateVariables.put("expenses", formattedExpenses);
        templateVariables.put("income", formattedIncome);
        templateVariables.put("currentYear", LocalDate.now().getYear());

        return templateVariables;
    }
}
