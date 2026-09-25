package net.focik.homeoffice.userservice.domain;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.audit.AuditableEntity;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
