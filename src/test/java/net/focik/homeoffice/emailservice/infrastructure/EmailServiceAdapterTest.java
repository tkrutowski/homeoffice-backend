package net.focik.homeoffice.emailservice.infrastructure;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy retry/logowania wprowadzonych przy naprawie EMAIL_SMTP_ISSUES.md #1-#3: retry po
 * chwilowym bledzie SMTP, oraz to, ze sendTemplatedEmail nie polyka wyniku wysylki w ciszy.
 * @Async jest tu bez znaczenia - w tescie jednostkowym wolamy metody bezposrednio na obiekcie,
 * z pominieciem proxy Springa, wiec wykonuja sie synchronicznie.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceAdapterTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templateEngine;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private EmailServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EmailServiceAdapter(mailSender, templateEngine, registry);
        adapter.mailFrom = "noreply@example.com";
        adapter.mailFromName = "Test App";
        adapter.retryBackoffMs = 0; // nie czekaj naprawde miedzy probami w testach
    }

    private double sent(String type, String result) {
        return registry.get(EmailServiceAdapter.SENT_METRIC).tag("type", type).tag("result", result).counter().count();
    }

    private static MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    @Test
    @DisplayName("should send the HTML email on the first attempt when there is no error")
    void sendHtmlEmail_ShouldSendOnFirstAttempt_WhenNoError() {
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

        adapter.sendHtmlEmail("to@example.com", "Subject", "<p>Hi</p>");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertThat(sent("html", "success")).isEqualTo(1);
        assertThat(sent("html", "failure")).isZero();
    }

    @Test
    @DisplayName("should retry and eventually succeed when the first SMTP attempts fail transiently")
    void sendHtmlEmail_ShouldRetryAndSucceed_WhenFirstAttemptsFailTransiently() {
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
        doThrow(new MailSendException("timeout"))
                .doThrow(new MailSendException("timeout"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        adapter.sendHtmlEmail("to@example.com", "Subject", "<p>Hi</p>");

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("should give up without throwing after all retry attempts fail")
    void sendHtmlEmail_ShouldGiveUpAndNotThrow_AfterMaxAttemptsAllFail() {
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
        doThrow(new MailSendException("timeout")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> adapter.sendHtmlEmail("to@example.com", "Subject", "<p>Hi</p>"))
                .doesNotThrowAnyException();

        verify(mailSender, times(3)).send(any(MimeMessage.class));
        // jedna wiadomosc = jedna porazka, a nie trzy (po jednej na probe)
        assertThat(sent("html", "failure")).isEqualTo(1);
        assertThat(sent("html", "success")).isZero();
    }

    @Test
    @DisplayName("should retry the simple email and succeed when the first attempt fails transiently")
    void sendSimpleEmail_ShouldRetryAndSucceed_WhenFirstAttemptFailsTransiently() {
        doThrow(new MailSendException("timeout"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        adapter.sendSimpleEmail("to@example.com", "Subject", "text");

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        assertThat(sent("simple", "success")).isEqualTo(1);
        assertThat(sent("simple", "failure")).isZero();
    }

    @Test
    @DisplayName("should send the templated email successfully when the template renders and sending succeeds")
    void sendTemplatedEmail_ShouldSendSuccessfully_WhenTemplateRendersAndSendSucceeds() {
        when(templateEngine.process(eq("emails/report.html"), any(Context.class))).thenReturn("<p>Report</p>");
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

        EmailRequest request = new EmailRequest("to@example.com", "Subject", "report.html", Map.of("x", 1));
        adapter.sendTemplatedEmail(request);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("should not throw when sending the templated email fails after all retries")
    void sendTemplatedEmail_ShouldNotThrow_WhenSendingFailsAfterAllRetries() {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<p>Report</p>");
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
        doThrow(new MailSendException("timeout")).when(mailSender).send(any(MimeMessage.class));

        EmailRequest request = new EmailRequest("to@example.com", "Subject", "report.html", Map.of());

        assertThatCode(() -> adapter.sendTemplatedEmail(request)).doesNotThrowAnyException();

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }
}
