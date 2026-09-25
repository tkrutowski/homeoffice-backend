package net.focik.homeoffice.userservice.domain.port.primary;

public interface RequestPasswordResetUseCase {

    void requestPasswordReset(String email);
}
