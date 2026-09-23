package net.focik.homeoffice.userservice.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.api.dto.AuthenticationResponse;
import net.focik.homeoffice.userservice.api.dto.GoogleLoginRequest;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.JwtService;
import net.focik.homeoffice.userservice.domain.exceptions.UserNotFoundException;
import net.focik.homeoffice.userservice.domain.port.primary.GetUserUseCase;
import net.focik.homeoffice.userservice.domain.port.secondary.IAppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Date;

/**
 * Logowanie przez Google - WYLACZNIE dla juz istniejacych, recznie zalozonych kont.
 * Ten flow nigdy nie tworzy nowego AppUser - jesli nie ma konta o e-mailu z tokena Google,
 * logowanie jest odrzucane.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final GetUserUseCase getUserUseCase;
    private final IAppUserRepository userRepository;
    private final JwtService jwtService;

    public AuthenticationResponse authenticate(GoogleLoginRequest request) {
        GoogleIdToken idToken = verify(request.getIdToken());

        GoogleIdToken.Payload payload = idToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BadCredentialsException("Google email not verified");
        }

        String email = payload.getEmail();
        String googleSub = payload.getSubject();

        AppUser user = getUserUseCase.findUserByEmail(email);
        if (user == null) {
            log.warn("Google login attempt for unknown email: {}", email);
            throw new UserNotFoundException("Brak konta powiazanego z tym adresem e-mail. Konta zakladane sa recznie przez administratora.");
        }

        //te same kontrole, ktore przy logowaniu haslem robi domyslnie AuthenticationManager/DaoAuthenticationProvider -
        //tutaj trzeba je powtorzyc recznie, bo AuthenticationManager jest pomijany
        if (!user.isEnabled()) {
            throw new DisabledException("Account disabled");
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account locked");
        }

        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleSub);
            log.info("Linked Google account to existing user: {}", user.getUsername());
        } else if (!user.getGoogleSub().equals(googleSub)) {
            //ten sam e-mail, ale inne konto Google niz zapisane wczesniej - odmawiamy zamiast cicho nadpisywac
            log.error("Google sub mismatch for user {}: stored={}, incoming={}", user.getUsername(), user.getGoogleSub(), googleSub);
            throw new BadCredentialsException("Google account mismatch");
        }

        //zdjecie profilowe z Google - odswiezane przy kazdym logowaniu, zeby nadazac za zmianami
        //po stronie Google; pole opcjonalne w tokenie, wiec moze nie wystapic
        Object picture = payload.get("picture");
        if (picture != null) {
            user.setAvatarUrl(picture.toString());
        }

        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        log.info("Success Google login for user: {}", user.getUsername());

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private GoogleIdToken verify(String rawIdToken) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(rawIdToken);
            if (idToken == null) {
                throw new BadCredentialsException("Invalid Google ID token");
            }
            return idToken;
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid Google ID token", e);
        }
    }
}
