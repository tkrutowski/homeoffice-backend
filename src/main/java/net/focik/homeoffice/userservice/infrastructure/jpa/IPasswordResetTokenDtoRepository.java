package net.focik.homeoffice.userservice.infrastructure.jpa;

import net.focik.homeoffice.userservice.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IPasswordResetTokenDtoRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);
}
