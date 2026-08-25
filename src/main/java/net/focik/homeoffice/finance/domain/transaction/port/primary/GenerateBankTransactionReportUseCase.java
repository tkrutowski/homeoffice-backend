package net.focik.homeoffice.finance.domain.transaction.port.primary;

/**
 * UseCase port for generating bank transaction reports
 * Handles monthly and weekly transaction summaries from bank accounts
 */
public interface GenerateBankTransactionReportUseCase {

    /**
     * Process and send monthly bank transaction reports for all users
     * Reports are sent for the previous month
     */
    void processMonthlyReports();

    /**
     * Process and send weekly bank transaction reports for all users
     * Reports are sent for the previous week
     */
    void processWeeklyReports();
}
