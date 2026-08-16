package net.focik.homeoffice.emailservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for email operations
 * Provides endpoints for testing email functionality
 * Available only to ADMIN users
 */
@Slf4j
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class EmailController {

    private final EmailNotificationPort emailNotificationPort;

    /**
     * Send a test simple email
     * POST /api/emails/test/simple
     *
     * @param request contains: to (recipient email), subject, text (body)
     * @return success message
     */
    @PostMapping("/test/simple")
    public ResponseEntity<Map<String, String>> sendTestSimpleEmail(@RequestBody EmailRequest request) {
        log.info("Sending test simple email to: {}", request.getTo());
        emailNotificationPort.sendSimpleEmail(request.getTo(), request.getSubject(), "Test email body");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Simple email queued for sending");
        response.put("to", request.getTo());
        return ResponseEntity.ok(response);
    }

    /**
     * Send a test HTML email
     * POST /api/emails/test/html
     *
     * @param request contains: to (recipient email), subject
     * @return success message
     */
    @PostMapping("/test/html")
    public ResponseEntity<Map<String, String>> sendTestHtmlEmail(@RequestBody EmailRequest request) {
        log.info("Sending test HTML email to: {}", request.getTo());

        String htmlContent = """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h1>🧪 Test HTML Email</h1>
                        <p>This is a test HTML email from HomeOffice App</p>
                        <p style="color: green;"><strong>✓ HTML formatting works correctly!</strong></p>
                    </body>
                </html>
                """;

        emailNotificationPort.sendHtmlEmail(request.getTo(), request.getSubject(), htmlContent);

        Map<String, String> response = new HashMap<>();
        response.put("message", "HTML email queued for sending");
        response.put("to", request.getTo());
        return ResponseEntity.ok(response);
    }

    /**
     * Send a test templated email
     * POST /api/emails/test/template
     *
     * @param request contains: to, subject, templateName, templateVariables
     * @return success message
     */
    @PostMapping("/test/template")
    public ResponseEntity<Map<String, String>> sendTestTemplatedEmail(@RequestBody EmailRequest request) {
        log.info("Sending test templated email to: {} using template: {}",
                request.getTo(), request.getTemplateName());

        emailNotificationPort.sendTemplatedEmail(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Templated email queued for sending");
        response.put("to", request.getTo());
        response.put("template", request.getTemplateName());
        return ResponseEntity.ok(response);
    }

}
