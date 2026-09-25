package net.focik.homeoffice.userservice.domain.port.secondary;

import net.focik.homeoffice.userservice.domain.PasswordResetToken;

import java.util.List;
import java.util.Optional;

public interface IPasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findActiveTokensByUserId(Long userId);
}
