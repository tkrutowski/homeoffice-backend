package net.focik.homeoffice.userservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.userservice.domain.exceptions.InvalidPasswordResetTokenException;
import net.focik.homeoffice.userservice.domain.exceptions.WeakPasswordException;
import net.focik.homeoffice.userservice.domain.port.primary.RequestPasswordResetUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.ResetPasswordUseCase;
import net.focik.homeoffice.userservice.domain.port.secondary.IAppUserRepository;
import net.focik.homeoffice.userservice.domain.port.secondary.IPasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static net.focik.homeoffice.userservice.domain.security.constant.UserConstant.*;

/**
 * Reset hasla przez link mailowy ("zapomnialem hasla"). Dwa UseCase (zadanie resetu + faktyczny
 * reset) sa tu polaczone w jednym Facade, bo obie operacje orkiestruja te same zaleznosci
 * (user repo, token repo, mail) i dziela logike haszowania/walidacji tokena.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetFacade implements RequestPasswordResetUseCase, ResetPasswordUseCase {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    //anty-spam bez pelnego rate limitera: nie generuj kolejnego tokena, jesli poprzedni
    //powstal chwile temu
    private static final Duration MIN_INTERVAL_BETWEEN_REQUESTS = Duration.ofMinutes(2);
    private static final int TOKEN_BYTES = 32;

    private final IAppUserRepository userRepository;
    private final IPasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationPort emailNotificationPort;

    @Value("${app.password-reset.frontend-url}")
    private String frontendUrl;

    @Override
    public void requestPasswordReset(String email) {
        AppUser user = userRepository.findUserByEmail(email);
        if (user == null) {
            //celowo bez wyjatku/roznicy w odpowiedzi - nie ujawniamy, czy konto istnieje
            log.info("Password reset requested for an email with no matching account");
            return;
        }

        List<PasswordResetToken> activeTokens = tokenRepository.findActiveTokensByUserId(user.getId());
        boolean requestedTooRecently = activeTokens.stream()
                .anyMatch(t -> t.getCreatedAt() != null
                        && t.getCreatedAt().isAfter(LocalDateTime.now().minus(MIN_INTERVAL_BETWEEN_REQUESTS)));
        if (requestedTooRecently) {
            log.info("Password reset requested too soon after a previous request for user id: {}", user.getId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        activeTokens.forEach(t -> t.setUsedAt(now));
        activeTokens.forEach(tokenRepository::save);

        String rawToken = generateRawToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(TOKEN_TTL))
                .build();
        tokenRepository.save(token);

        sendResetEmail(user, rawToken);
        log.info("Password reset email queued for user id: {}", user.getId());
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new InvalidPasswordResetTokenException(INVALID_RESET_TOKEN));

        if (!PASSWORD_POLICY.matcher(newPassword).matches()) {
            throw new WeakPasswordException(WEAK_PASSWORD);
        }

        AppUser user = userRepository.findUserById(resetToken.getUserId());
        if (user == null) {
            throw new InvalidPasswordResetTokenException(INVALID_RESET_TOKEN);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user id: {}", user.getId());
    }

    private void sendResetEmail(AppUser user, String rawToken) {
        String resetLink = frontendUrl + "?token=" + rawToken;
        String displayName = user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName() : user.getUsername();

        EmailRequest emailRequest = new EmailRequest(
                user.getEmail(),
                "Reset hasła - HomeOffice",
                "password-reset.html",
                Map.of(
                        "userName", displayName,
                        "resetLink", resetLink,
                        "expiryMinutes", TOKEN_TTL.toMinutes(),
                        "currentYear", Year.now().getValue()
                )
        );
        emailNotificationPort.sendTemplatedEmail(emailRequest);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
