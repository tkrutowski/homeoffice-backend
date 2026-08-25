package net.focik.homeoffice.finance.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GeneratePurchaseReportUseCase;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GenerateBankTransactionReportUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for testing expense reports
 * Handles both bank transaction reports and purchase (credit card) reports
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/finance/reports")
@RequiredArgsConstructor
public class ExpenseReportController {

    private final GenerateBankTransactionReportUseCase generateBankTransactionReportUseCase;
    private final GeneratePurchaseReportUseCase generatePurchaseReportUseCase;

    /**
     * Trigger monthly bank transaction reports
     */
    @PostMapping("/bank-transactions/monthly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> processBankTransactionMonthlyReports() {
        try {
            log.info("Manual trigger of monthly bank transaction reports");
            generateBankTransactionReportUseCase.processMonthlyReports();
            return ResponseEntity.ok("Monthly bank transaction reports processed successfully");
        } catch (Exception e) {
            log.error("Error processing monthly bank transaction reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing monthly bank transaction reports: " + e.getMessage());
        }
    }

    /**
     * Trigger weekly bank transaction reports
     */
    @PostMapping("/bank-transactions/weekly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> processBankTransactionWeeklyReports() {
        try {
            log.info("Manual trigger of weekly bank transaction reports");
            generateBankTransactionReportUseCase.processWeeklyReports();
            return ResponseEntity.ok("Weekly bank transaction reports processed successfully");
        } catch (Exception e) {
            log.error("Error processing weekly bank transaction reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing weekly bank transaction reports: " + e.getMessage());
        }
    }

    /**
     * Trigger monthly purchase (credit card) reports
     */
    @PostMapping("/purchases/monthly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> processPurchaseMonthlyReports() {
        try {
            log.info("Manual trigger of monthly purchase reports");
            generatePurchaseReportUseCase.processMonthlyReports();
            return ResponseEntity.ok("Monthly purchase reports processed successfully");
        } catch (Exception e) {
            log.error("Error processing monthly purchase reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing monthly purchase reports: " + e.getMessage());
        }
    }

    /**
     * Trigger weekly purchase (credit card) reports
     */
    @PostMapping("/purchases/weekly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> processPurchaseWeeklyReports() {
        try {
            log.info("Manual trigger of weekly purchase reports");
            generatePurchaseReportUseCase.processWeeklyReports();
            return ResponseEntity.ok("Weekly purchase reports processed successfully");
        } catch (Exception e) {
            log.error("Error processing weekly purchase reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing weekly purchase reports: " + e.getMessage());
        }
    }

    /**
     * Trigger all reports (monthly, both transaction and purchase)
     */
    @PostMapping("/all/monthly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> processAllMonthlyReports() {
        try {
            log.info("Manual trigger of all monthly reports");
            generateBankTransactionReportUseCase.processMonthlyReports();
            generatePurchaseReportUseCase.processMonthlyReports();
            return ResponseEntity.ok("All monthly reports processed successfully");
        } catch (Exception e) {
            log.error("Error processing all monthly reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing all monthly reports: " + e.getMessage());
        }
    }

    /**
     * Trigger all reports (monthly and weekly, both transaction and purchase)
     */
    @PostMapping("/all/weekly")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'FINANCE_PAYMENT_READ_ALL')")
    public ResponseEntity<String> processAllWeeklyReports() {
        try {
            log.info("Manual trigger of all weekly reports");
            generateBankTransactionReportUseCase.processWeeklyReports();
            generatePurchaseReportUseCase.processWeeklyReports();
            return ResponseEntity.ok("All weekly reports processed successfully");
        } catch (Exception e) {
            log.error("Error processing all weekly reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing all weekly reports: " + e.getMessage());
        }
    }
}
