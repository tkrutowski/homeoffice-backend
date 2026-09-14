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

    /** Zob. EMAIL_SMTP_ISSUES.md #1 - proba, potem do 2 dodatkowych prob po chwilowym bledzie SMTP. */
    private static final int MAX_SEND_ATTEMPTS = 3;
    /** Pakietowo-widoczne (nie final) wylacznie po to, zeby testy mogly przyspieszyc retry. */
    long retryBackoffMs = 2000;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    // Pakietowo-widoczne (nie private), zeby testy jednostkowe mogly je ustawic bezposrednio -
    // ta klasa normalnie dostaje je przez Spring @Value, nie ma innego konstruktora do tego celu.
    @Value("${app.mail.from}")
    String mailFrom;

    @Value("${app.mail.from-name}")
    String mailFromName;

    /**
     * Send a simple text email asynchronously
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendSimpleEmail(String to, String subject, String text) {
        doSendSimpleEmail(to, subject, text);
    }

    /**
     * Send an HTML formatted email asynchronously
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        doSendHtmlEmail(to, subject, htmlContent);
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

            // Send as HTML email - wolane bezposrednio (nie przez this.sendHtmlEmail), zeby
            // dostac faktyczny wynik wysylki i zeby nie polegac na @Async przy self-invocation,
            // ktore Spring AOP i tak by zignorowal (zob. EMAIL_SMTP_ISSUES.md #3) - nieszkodliwe,
            // bo i tak jestesmy juz w tle dzieki @Async na tej metodzie, ale mylace bez potrzeby.
            boolean sent = doSendHtmlEmail(request.getTo(), request.getSubject(), htmlContent);
            if (sent) {
                log.debug("Templated email processed and sent from template: {}", request.getTemplateName());
            } else {
                log.warn("Templated email processed but NOT sent (see error above) - template: {}, to: {}",
                        request.getTemplateName(), request.getTo());
            }
        } catch (Exception e) {
            log.error("Failed to send templated email to: {} using template: {}",
                    request.getTo(), request.getTemplateName(), e);
        }
    }

    /**
     * Faktyczna wysylka prostego maila z retry - zwraca, czy sie udalo, zamiast polykac wynik
     * w ciszy (zob. EMAIL_SMTP_ISSUES.md #2).
     */
    private boolean doSendSimpleEmail(String to, String subject, String text) {
        for (int attempt = 1; attempt <= MAX_SEND_ATTEMPTS; attempt++) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
                log.info("Simple email sent successfully to: {} | Subject: {}", to, subject);
                return true;
            } catch (MailException e) {
                if (attempt == MAX_SEND_ATTEMPTS) {
                    log.error("Failed to send simple email to: {} after {} attempt(s)", to, attempt, e);
                    return false;
                }
                log.warn("Attempt {}/{} failed sending simple email to: {} - retrying: {}",
                        attempt, MAX_SEND_ATTEMPTS, to, e.getMessage());
                sleepBeforeRetry();
            }
        }
        return false;
    }

    /**
     * Faktyczna wysylka HTML maila z retry - dzielona przez sendHtmlEmail i sendTemplatedEmail,
     * zamiast tego, zeby sendTemplatedEmail wolalo sendHtmlEmail (self-invocation, patrz wyzej).
     * Zwraca, czy sie udalo.
     */
    private boolean doSendHtmlEmail(String to, String subject, String htmlContent) {
        for (int attempt = 1; attempt <= MAX_SEND_ATTEMPTS; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(mailFrom, mailFromName);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true); // true = isHtml
                mailSender.send(message);
                log.info("HTML email sent successfully to: {} | Subject: {}", to, subject);
                return true;
            } catch (MessagingException | MailException e) {
                if (attempt == MAX_SEND_ATTEMPTS) {
                    log.error("Failed to send HTML email to: {} after {} attempt(s)", to, attempt, e);
                    return false;
                }
                log.warn("Attempt {}/{} failed sending HTML email to: {} - retrying: {}",
                        attempt, MAX_SEND_ATTEMPTS, to, e.getMessage());
                sleepBeforeRetry();
            } catch (Exception e) {
                log.error("Unexpected error while sending HTML email to: {}", to, e);
                return false;
            }
        }
        return false;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
