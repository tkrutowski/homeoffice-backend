package net.focik.homeoffice.emailservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Adapter implementing EmailNotificationPort using Spring Mail and Thymeleaf
 * Provides functionality to send simple text, HTML, and templated emails asynchronously
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceAdapter implements EmailNotificationPort {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.from-name}")
    private String mailFromName;

    /**
     * Send a simple text email asynchronously
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Simple email sent successfully to: {} | Subject: {}", to, subject);
        } catch (MailException e) {
            log.error("Failed to send simple email to: {}", to, e);
        }
    }

    /**
     * Send an HTML formatted email asynchronously
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml
            mailSender.send(message);
            log.info("HTML email sent successfully to: {} | Subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
        } catch (MailException e) {
            log.error("Mail exception while sending HTML email to: {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected error while sending HTML email to: {}", to, e);
        }
    }

    /**
     * Send a templated email using Thymeleaf template engine
     * The template name should correspond to a file in src/main/resources/templates/emails/
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendTemplatedEmail(EmailRequest request) {
        try {
            // Prepare Thymeleaf context with template variables
            Context context = new Context();
            context.setVariables(request.getTemplateVariables());

            // Process template
            String htmlContent = templateEngine.process(
                    "emails/" + request.getTemplateName(),
                    context
            );

            // Send as HTML email
            sendHtmlEmail(request.getTo(), request.getSubject(), htmlContent);
            log.debug("Templated email processed and sent from template: {}", request.getTemplateName());
        } catch (Exception e) {
            log.error("Failed to send templated email to: {} using template: {}",
                    request.getTo(), request.getTemplateName(), e);
        }
    }
}
