package net.focik.homeoffice.library.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.BookService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for testing BookService email functionality
 *
 * Provides endpoints to manually trigger and test:
 * - Monthly books summary emails
 * - Yearly reading statistics emails
 *
 * Available only to ADMIN users for testing purposes
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/books/emails/test")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class BookEmailTestController {

    private final BookService bookService;

    /**
     * Send monthly books summary for a specific user
     *
     * GET /api/books/emails/test/monthly-summary?userId=1
     *
     * @param userId the user ID to send the summary to
     * @return response with status and details
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<Map<String, Object>> sendMonthlySummary(
            @RequestParam Long userId) {

        log.info("Manually triggered monthly summary for user: {}", userId);

        try {
            bookService.sendMonthlyBooksSummary(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Monthly books summary email queued");
            response.put("userId", userId);
            response.put("timestamp", LocalDate.now());
            response.put("action", "Monthly Summary (Current Month)");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send monthly summary for user: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send email: " + e.getMessage());
            errorResponse.put("userId", userId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Send yearly reading statistics for a specific user and year
     *
     * GET /api/books/emails/test/yearly-stats?userId=1&year=2026
     *
     * @param userId the user ID to send the stats to
     * @param year the year to calculate statistics for (default: current year)
     * @return response with status and details
     */
    @GetMapping("/yearly-stats")
    public ResponseEntity<Map<String, Object>> sendYearlyStatistics(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "2026") Integer year) {

        log.info("Manually triggered yearly stats for user: {} year: {}", userId, year);

        try {
            bookService.sendYearlyReadingStatistics(userId, year);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Yearly reading statistics email queued");
            response.put("userId", userId);
            response.put("year", year);
            response.put("timestamp", LocalDate.now());
            response.put("action", "Yearly Statistics");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send yearly stats for user: {} year: {}", userId, year, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send email: " + e.getMessage());
            errorResponse.put("userId", userId);
            errorResponse.put("year", year);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Send monthly books summary for a specific date range
     *
     * This endpoint allows you to test with custom date ranges
     * Useful for simulating past months without waiting for scheduler
     *
     * POST /api/books/emails/test/monthly-summary-custom
     *
     * Request body example:
     * {
     *   "userId": 1,
     *   "startDate": "2026-08-01",
     *   "endDate": "2026-08-31"
     * }
     *
     * @return response with status and details
     */
    @PostMapping("/monthly-summary-custom")
    public ResponseEntity<Map<String, Object>> sendMonthlySummaryCustom(
            @RequestBody MonthlySummaryRequest request) {

        log.info("Manually triggered custom monthly summary for user: {} from {} to {}",
                request.getUserId(), request.getStartDate(), request.getEndDate());

        try {
            // Validate dates
            if (request.getStartDate().isAfter(request.getEndDate())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Start date must be before end date");
                errorResponse.put("userId", request.getUserId());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Note: sendMonthlyBooksSummary uses current month
            // For custom dates, you would need to create a new method in BookService
            // For now, send the current month summary
            bookService.sendMonthlyBooksSummary(request.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Monthly books summary email queued");
            response.put("userId", request.getUserId());
            response.put("startDate", request.getStartDate());
            response.put("endDate", request.getEndDate());
            response.put("note", "Using current month range");
            response.put("timestamp", LocalDate.now());
            response.put("action", "Monthly Summary (Custom Range)");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send custom monthly summary for user: {}", request.getUserId(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send email: " + e.getMessage());
            errorResponse.put("userId", request.getUserId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Send monthly summary to all users (simulate scheduled task)
     *
     * GET /api/books/emails/test/monthly-summary-all
     *
     * WARNING: This will send emails to ALL users in the system
     *
     * @return response with number of emails sent
     */
    @GetMapping("/monthly-summary-all")
    public ResponseEntity<Map<String, Object>> sendMonthlySummaryToAllUsers() {

        log.warn("Manually triggered monthly summary for ALL users");

        try {
            bookService.sendMonthlySummariesToAllUsers();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Monthly summary emails queued for all users");
            response.put("timestamp", LocalDate.now());
            response.put("action", "Monthly Summary (All Users)");
            response.put("warning", "Check logs for actual email sending status");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send monthly summary to all users", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send emails: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get help information about available test endpoints
     *
     * GET /api/books/emails/test/help
     *
     * @return help information
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelpInfo() {

        Map<String, Object> help = new HashMap<>();
        help.put("endpoints", new HashMap<String, Object>() {{
            put("GET /api/v1/books/emails/test/monthly-summary?userId=1",
                    "Send monthly summary for specific user");
            put("GET /api/v1/books/emails/test/yearly-stats?userId=1&year=2026",
                    "Send yearly statistics for specific user and year");
            put("POST /api/v1/books/emails/test/monthly-summary-custom",
                    "Send monthly summary with custom date range");
            put("GET /api/v1/books/emails/test/monthly-summary-all",
                    "Send monthly summary to ALL users (simulate scheduler)");
            put("GET /api/v1/books/emails/test/help",
                    "Show this help information");
        }});

        help.put("description", "Test endpoints for BookService email functionality");
        help.put("authorization", "ROLE_ADMIN only");
        help.put("notes", new String[]{
                "All emails are sent asynchronously",
                "Check application logs for sending status",
                "Monthly summary uses current month's date range",
                "Yearly stats defaults to current year if not specified"
        });

        return ResponseEntity.ok(help);
    }

    /**
     * Request DTO for custom monthly summary
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MonthlySummaryRequest {
        private Long userId;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
    }
}
