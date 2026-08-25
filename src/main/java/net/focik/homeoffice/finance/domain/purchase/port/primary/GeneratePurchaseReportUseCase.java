package net.focik.homeoffice.finance.domain.purchase.port.primary;

/**
 * UseCase port for generating purchase reports
 * Handles monthly and weekly purchase summaries from credit card purchases
 */
public interface GeneratePurchaseReportUseCase {

    /**
     * Process and send monthly purchase reports for all users
     * Reports are sent for the previous month
     */
    void processMonthlyReports();

    /**
     * Process and send weekly purchase reports for all users
     * Reports are sent for the previous week
     */
    void processWeeklyReports();
}
