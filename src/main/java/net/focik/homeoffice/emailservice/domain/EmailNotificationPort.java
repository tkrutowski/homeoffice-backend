package net.focik.homeoffice.emailservice.domain;

/**
 * Port interface for email sending operations
 * Allows sending simple text emails, HTML emails, and templated emails
 */
public interface EmailNotificationPort {

    /**
     * Send a simple text email
     * @param to recipient email address
     * @param subject email subject
     * @param text email body text
     */
    void sendSimpleEmail(String to, String subject, String text);

    /**
     * Send an HTML formatted email
     * @param to recipient email address
     * @param subject email subject
     * @param htmlContent email body in HTML format
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);

    /**
     * Send a templated email using Thymeleaf templates
     * @param request EmailRequest containing template name and variables
     */
    void sendTemplatedEmail(EmailRequest request);
}
