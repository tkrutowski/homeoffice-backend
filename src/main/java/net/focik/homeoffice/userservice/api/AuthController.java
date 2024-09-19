package net.focik.homeoffice.userservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.api.dto.AuthenticationRequest;
import net.focik.homeoffice.userservice.api.dto.AuthenticationResponse;
import net.focik.homeoffice.userservice.api.dto.RefreshRequest;
import net.focik.homeoffice.userservice.application.AuthenticationService;
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

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest authenticationRequest) {
        log.info("Login attempt for user: {}", authenticationRequest.getUsername());
        return ResponseEntity.ok(authenticationService.authenticate(authenticationRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody RefreshRequest refreshRequest) throws IOException {
        log.info("Attempt to refresh token");
        AuthenticationResponse refreshedToken = authenticationService.refreshToken(refreshRequest);
        return ResponseEntity.ok(refreshedToken);
    }
}
