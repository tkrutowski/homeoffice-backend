package net.focik.homeoffice.finance.domain.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.finance.domain.card.CardFacade;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GeneratePurchaseReportUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for managing purchase reports (credit card payments)
 * Handles business logic for sending monthly and weekly purchase summaries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseReportService implements GeneratePurchaseReportUseCase {

    private final GetPurchaseUseCase getPurchaseUseCase;
    private final CardFacade cardFacade;
    private final EmailNotificationPort emailNotificationPort;
    private final UserFacade userFacade;

    @Value("${app.purchase-report.enabled:true}")
    private boolean purchaseReportEnabled;

    /**
     * Scheduled task to process monthly purchase reports
     * Runs on the 1st day of each month at 10:00 AM
     */
    @Scheduled(cron = "${app.purchase-report.monthly.cron:0 0 10 1 * *}")
    public void scheduledProcessMonthlyReports() {
        if (!purchaseReportEnabled) {
            log.debug("Purchase reports are disabled");
            return;
        }

        try {
            log.debug("Triggering scheduled monthly purchase report processing");
            processMonthlyReports();
        } catch (Exception e) {
            log.error("Error in scheduled monthly purchase report processing", e);
        }
    }

    /**
     * Scheduled task to process weekly purchase reports
     * Runs every Monday at 9:30 AM
     */
    @Scheduled(cron = "${app.purchase-report.weekly.cron:0 30 9 ? * MON}")
    public void scheduledProcessWeeklyReports() {
        if (!purchaseReportEnabled) {
            log.debug("Purchase reports are disabled");
            return;
        }

        try {
            log.debug("Triggering scheduled weekly purchase report processing");
            processWeeklyReports();
        } catch (Exception e) {
            log.error("Error in scheduled weekly purchase report processing", e);
        }
    }

    /**
     * Process and send monthly purchase reports for all users
     */
    public void processMonthlyReports() {
        log.info("Starting monthly purchase report processing");

        try {
            LocalDate today = LocalDate.now();
            LocalDate firstDayOfPreviousMonth = today.minusMonths(1)
                    .with(TemporalAdjusters.firstDayOfMonth());
            LocalDate lastDayOfPreviousMonth = today.minusMonths(1)
                    .with(TemporalAdjusters.lastDayOfMonth());

            List<AppUser> allUsers = userFacade.getAllUsers();
            for (AppUser user : allUsers) {
                try {
                    List<Purchase> allPurchases = getPurchaseUseCase.findByUser(
                            user.getUsername(),
                            null,
                            firstDayOfPreviousMonth
                    );

                    // Filter to only include purchases from previous month
                    List<Purchase> purchases = allPurchases.stream()
                            .filter(p -> !p.getPurchaseDate().isBefore(firstDayOfPreviousMonth) &&
                                    !p.getPurchaseDate().isAfter(lastDayOfPreviousMonth))
                            .collect(Collectors.toList());

                    if (!purchases.isEmpty()) {
                        sendMonthlyReportEmail(user, purchases, firstDayOfPreviousMonth);
                    }
                } catch (Exception e) {
                    log.error("Error processing monthly purchase report for user: {}", user.getUsername(), e);
                }
            }

            log.info("Monthly purchase report processing completed successfully");
        } catch (Exception e) {
            log.error("Error during monthly purchase report processing", e);
        }
    }

    /**
     * Process and send weekly purchase reports for all users
     */
    public void processWeeklyReports() {
        log.info("Starting weekly purchase report processing");

        try {
            LocalDate today = LocalDate.now();
            LocalDate lastSunday = today.minusDays(today.getDayOfWeek().getValue() % 7);
            LocalDate previousMonday = lastSunday.minusDays(6);

            List<AppUser> allUsers = userFacade.getAllUsers();
            for (AppUser user : allUsers) {
                try {
                    // Get purchases from previous week
                    List<Purchase> purchases = getPurchaseUseCase.findByUser(user.getUsername(), null, previousMonday);
                    purchases = purchases.stream()
                            .filter(p -> !p.getPurchaseDate().isBefore(previousMonday) && p.getPurchaseDate().isBefore(lastSunday.plusDays(1)))
                            .collect(Collectors.toList());

                    if (!purchases.isEmpty()) {
                        sendWeeklyReportEmail(user, purchases, previousMonday, lastSunday);
                    }
                } catch (Exception e) {
                    log.error("Error processing weekly purchase report for user: {}", user.getUsername(), e);
                }
            }

            log.info("Weekly purchase report processing completed successfully");
        } catch (Exception e) {
            log.error("Error during weekly purchase report processing", e);
        }
    }

    /**
     * Send monthly purchase report email
     */
    private void sendMonthlyReportEmail(AppUser user, List<Purchase> purchases, LocalDate monthDate) {
        if (user == null || user.getEmail() == null) {
            log.warn("User not found or has no email");
            return;
        }

        YearMonth month = YearMonth.from(monthDate);
        Map<String, Object> templateVariables = prepareTemplateVariables(
                user,
                purchases,
                month.toString()
        );

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(user.getEmail());
        emailRequest.setSubject("Miesięczne podsumowanie zakupów - " + month);
        emailRequest.setTemplateName("expense-report.html");
        emailRequest.setTemplateVariables(templateVariables);

        emailNotificationPort.sendTemplatedEmail(emailRequest);
    }

    /**
     * Send weekly purchase report email
     */
    private void sendWeeklyReportEmail(AppUser user, List<Purchase> purchases, LocalDate weekStart, LocalDate weekEnd) {
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
                purchases,
                weekLabel
        );

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(user.getEmail());
        emailRequest.setSubject("Tygodniowe podsumowanie zakupów - " + weekLabel);
        emailRequest.setTemplateName("expense-report.html");
        emailRequest.setTemplateVariables(templateVariables);

        emailNotificationPort.sendTemplatedEmail(emailRequest);
    }

    /**
     * Prepare template variables for purchase report email
     */
    private Map<String, Object> prepareTemplateVariables(
            AppUser user,
            List<Purchase> purchases,
            String period) {

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        templateVariables.put("month", "Zakupy kartą kredytową - " + period);

        // Calculate total amount
        Money totalAmount = purchases.stream()
                .map(p -> Money.of(p.getAmount(), "PLN"))
                .reduce(Money.of(0, "PLN"), Money::add);

        templateVariables.put("totalAmount", MoneyUtils.mapMoneyToString(totalAmount));

        // Calculate average amount
        if (!purchases.isEmpty()) {
            Money averageAmount = Money.of(
                    totalAmount.getNumber().doubleValue() / purchases.size(),
                    "PLN"
            );
            templateVariables.put("averageAmount", MoneyUtils.mapMoneyToString(averageAmount));
        } else {
            templateVariables.put("averageAmount", "0,00 zł");
        }

        // For purchase reports, income is always empty (purchases are expenses only)
        templateVariables.put("totalIncome", "0,00 zł");
        templateVariables.put("averageIncome", "0,00 zł");

        // Group purchases by card and format for display
        Map<String, Map<String, Object>> purchasesByCard = new LinkedHashMap<>();

        for (Purchase purchase : purchases) {
            try {
                String cardName = cardFacade.findById(purchase.getIdCard()).getCardName();

                Map<String, Object> purchaseMap = new HashMap<>();
                purchaseMap.put("description", purchase.getName());
                purchaseMap.put("entryDate", purchase.getPurchaseDate());
                purchaseMap.put("amount", MoneyUtils.mapMoneyToString(Money.of(purchase.getAmount(), "PLN")));

                purchasesByCard.computeIfAbsent(cardName, k -> {
                    Map<String, Object> cardData = new HashMap<>();
                    cardData.put("purchases", new ArrayList<Map<String, Object>>());
                    cardData.put("total", Money.of(0, "PLN"));
                    return cardData;
                });

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cardPurchases = (List<Map<String, Object>>) purchasesByCard.get(cardName).get("purchases");
                cardPurchases.add(purchaseMap);

                // Update total for this card
                Money cardTotal = (Money) purchasesByCard.get(cardName).get("total");
                Money purchaseAmount = Money.of(purchase.getAmount(), "PLN");
                purchasesByCard.get(cardName).put("total", cardTotal.add(purchaseAmount));
            } catch (Exception e) {
                log.warn("Could not find card with id: {}, skipping purchase: {}", purchase.getIdCard(), purchase.getId(), e);
            }
        }

        // Convert Money totals to formatted strings
        purchasesByCard.forEach((cardName, cardData) -> {
            Money total = (Money) cardData.get("total");
            cardData.put("totalFormatted", MoneyUtils.mapMoneyToString(total));
        });

        templateVariables.put("purchasesByCard", purchasesByCard);
        templateVariables.put("currentYear", LocalDate.now().getYear());

        return templateVariables;
    }
}
