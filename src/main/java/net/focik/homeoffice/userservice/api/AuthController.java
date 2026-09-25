package net.focik.homeoffice.userservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.api.dto.AuthenticationRequest;
import net.focik.homeoffice.userservice.api.dto.AuthenticationResponse;
import net.focik.homeoffice.userservice.api.dto.ForgotPasswordRequest;
import net.focik.homeoffice.userservice.api.dto.GoogleLoginRequest;
import net.focik.homeoffice.userservice.api.dto.RefreshRequest;
import net.focik.homeoffice.userservice.api.dto.ResetPasswordRequest;
import net.focik.homeoffice.userservice.application.AuthenticationService;
import net.focik.homeoffice.userservice.application.GoogleAuthService;
import net.focik.homeoffice.userservice.domain.port.primary.RequestPasswordResetUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.ResetPasswordUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

//@CrossOrigin
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = {"/api/v1/auth"})
public class AuthController {

    private final AuthenticationService authenticationService;
    private final GoogleAuthService googleAuthService;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest authenticationRequest) {
        log.info("Login attempt for user: {}", authenticationRequest.getUsername());
        return ResponseEntity.ok(authenticationService.authenticate(authenticationRequest));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> loginWithGoogle(@RequestBody GoogleLoginRequest googleLoginRequest) {
        log.info("Google login attempt");
        return ResponseEntity.ok(googleAuthService.authenticate(googleLoginRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody RefreshRequest refreshRequest) {
        log.info("Attempt to refresh token");
        AuthenticationResponse refreshedToken = authenticationService.refreshToken(refreshRequest);
        return ResponseEntity.ok(refreshedToken);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested");
        requestPasswordResetUseCase.requestPasswordReset(request.getEmail());
        //zawsze 200 OK, niezaleznie czy konto o tym e-mailu istnieje - anty-enumeracja kont
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt");
        resetPasswordUseCase.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Test ping");
        return ResponseEntity.ok("OK");
    }
}
