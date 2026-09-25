package net.focik.homeoffice.userservice.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.PasswordResetToken;
import net.focik.homeoffice.userservice.domain.port.secondary.IPasswordResetTokenRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements IPasswordResetTokenRepository {

    private final IPasswordResetTokenDtoRepository passwordResetTokenDtoRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return passwordResetTokenDtoRepository.save(token);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return passwordResetTokenDtoRepository.findByTokenHash(tokenHash);
    }

    @Override
    public List<PasswordResetToken> findActiveTokensByUserId(Long userId) {
        return passwordResetTokenDtoRepository.findByUserIdAndUsedAtIsNull(userId);
    }
}
