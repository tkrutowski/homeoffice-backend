package net.focik.homeoffice.userservice.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.api.dto.AuthenticationResponse;
import net.focik.homeoffice.userservice.api.dto.PasskeyDto;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.JwtService;
import net.focik.homeoffice.userservice.domain.exceptions.UserNotFoundException;
import net.focik.homeoffice.userservice.domain.port.primary.GetUserUseCase;
import net.focik.homeoffice.userservice.domain.port.secondary.IAppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * Domyslny endpoint Spring Security "POST /login/webauthn" po udanym logowaniu passkeyem
 * zwraca wlasny format ({@code {redirectUrl, authenticated:true}}), a nie nasze JWT - DSL
 * .webAuthn() nie daje prostego haka na podmiane tej odpowiedzi. Zamiast tego frontend, zaraz
 * po otrzymaniu {@code authenticated:true}, wywoluje ten endpoint (na tej samej sesji/ciasteczku
 * - patrz {@link WebAuthnSecurityConfig}) - tu czytamy juz ustawiony Authentication i wystawiamy
 * te same JWT co przy logowaniu haslem/Google.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = {"/webauthn"})
public class WebAuthnAuthController {

    private final GetUserUseCase getUserUseCase;
    private final IAppUserRepository userRepository;
    private final JwtService jwtService;
    private final PublicKeyCredentialUserEntityRepository webAuthnUserEntityRepository;
    private final UserCredentialRepository webAuthnUserCredentialRepository;

    //Spring Security WebAuthn nie ma wbudowanego GET-a do listowania zarejestrowanych kluczy
    //(ma tylko POST /webauthn/register i DELETE /webauthn/register/{id}) - potrzebny do sekcji
    //"passkeys" w ustawieniach konta (lista + przycisk usuwania), wiec dopisujemy go sami.
    @GetMapping("/register")
    public ResponseEntity<List<PasskeyDto>> listMyPasskeys(Authentication authentication) {
        String username = authentication.getName();
        PublicKeyCredentialUserEntity userEntity = webAuthnUserEntityRepository.findByUsername(username);
        if (userEntity == null) {
            //user nigdy nie zarejestrowal zadnego klucza - jeszcze nie ma wpisu w user_entities
            return ResponseEntity.ok(List.of());
        }

        List<PasskeyDto> passkeys = webAuthnUserCredentialRepository.findByUserId(userEntity.getId()).stream()
                .map(credential -> PasskeyDto.builder()
                        .id(credential.getCredentialId().toBase64UrlString())
                        .label(credential.getLabel())
                        .created(credential.getCreated())
                        .lastUsed(credential.getLastUsed())
                        .build())
                .toList();

        return ResponseEntity.ok(passkeys);
    }

    @PostMapping("/token")
    public ResponseEntity<AuthenticationResponse> exchangeSessionForJwt(Authentication authentication, HttpServletRequest request) {
        String username = authentication.getName();
        AppUser user = getUserUseCase.findUserByUsername(username);
        if (user == null) {
            //nie powinno sie zdarzyc - authentication.getName() pochodzi z klucza zarejestrowanego
            //dla istniejacego, zalogowanego uzytkownika, ale zabezpieczamy sie na wszelki wypadek
            log.error("WebAuthn session authenticated as unknown username: {}", username);
            throw new UserNotFoundException("Nie znaleziono uzytkownika dla zalogowanej sesji WebAuthn");
        }

        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        log.info("Success passkey login for user: {}", user.getUsername());

        //sesja posluzyla tylko do przekazania wyniku ceremonii WebAuthn - od tego momentu
        //uwierzytelnianie idzie juz przez JWT jak wszedzie indziej, sesja nie jest juz potrzebna
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build());
    }
}
