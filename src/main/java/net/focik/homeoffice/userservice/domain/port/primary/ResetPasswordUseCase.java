package net.focik.homeoffice.userservice.domain.port.primary;

public interface ResetPasswordUseCase {

    void resetPassword(String token, String newPassword);
}
