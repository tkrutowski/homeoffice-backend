package net.focik.homeoffice.userservice.domain.security.config;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.security.filter.JwtAccessDeniedHandler;
import net.focik.homeoffice.userservice.domain.security.filter.JwtAuthenticationEntryPoint;
import net.focik.homeoffice.userservice.domain.security.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;

/**
 * Osobny SecurityFilterChain dla passkeys/WebAuthn (Faza 2 logowania - patrz docs/PASSKEYS_LOGIN.md).
 * <p>
 * Ceremonia WebAuthn (rejestracja i logowanie) wymaga zapamietania "challenge" na serwerze
 * pomiedzy dwoma requestami (.../options -> .../register albo .../authenticate) - domyslnie
 * Spring trzyma ten stan w HttpSession. Glowny lancuch API ({@link SecurityConfig}) jest
 * celowo STATELESS, wiec ta ceremonia dostaje wlasny, wezszy lancuch (tylko /webauthn/** i
 * /login/webauthn) z sesja IF_REQUIRED - reszta API nie jest tym ruszana.
 * <p>
 * Rejestracja nowego klucza (/webauthn/register/options, /webauthn/register, DELETE
 * /webauthn/register/{id}) wymaga bycia juz zalogowanym - stad jwtAuthFilter jest dolaczony
 * i do TEGO lancucha, zeby "Authorization: Bearer &lt;JWT&gt;" dzialal tu tak samo jak w
 * glownym API. Logowanie (/webauthn/authenticate/options, /login/webauthn) jest celowo
 * publiczne - to alternatywa dla /api/v1/auth/login, a nie cos wymagajace uprzedniego logowania.
 * <p>
 * Spring nie daje gotowego haka na podmiane domyslnej odpowiedzi po /login/webauthn (zwraca
 * {redirectUrl, authenticated:true} zamiast naszych JWT) - dlatego frontend po otrzymaniu
 * authenticated:true musi dodatkowo wywolac POST /webauthn/token (patrz WebAuthnAuthController),
 * ktory czyta juz ustawiony w sesji Authentication i wystawia nasze wlasne
 * accessToken/refreshToken, tak samo jak przy logowaniu haslem/Google.
 */
@Configuration
@RequiredArgsConstructor
public class WebAuthnSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${webauthn.rp-id}")
    private String rpId;
    @Value("${webauthn.rp-name}")
    private String rpName;
    @Value("${webauthn.allowed-origins}")
    private String allowedOriginsProperty;

    //persystencja kluczy w bazie (bez tego biblioteka domyslnie trzyma je tylko w pamieci -
    //zniknelyby po kazdym restarcie aplikacji). Nazwy tabel/kolumn - patrz migracja V7.
    @Bean
    public JdbcPublicKeyCredentialUserEntityRepository webAuthnUserEntityRepository(JdbcOperations jdbcOperations) {
        return new JdbcPublicKeyCredentialUserEntityRepository(jdbcOperations);
    }

    @Bean
    public JdbcUserCredentialRepository webAuthnUserCredentialRepository(JdbcOperations jdbcOperations) {
        return new JdbcUserCredentialRepository(jdbcOperations);
    }

    //Order(1): musi byc oceniany PRZED glownym lancuchem (SecurityConfig, Order(2)), bo ten tu
    //dopasowuje wezszy zakres (/webauthn/**, /login/webauthn) w ramach tego samego anyRequest().
    @Bean
    @Order(1)
    public SecurityFilterChain webAuthnSecurityFilterChain(HttpSecurity http) {
        List<String> allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        try {
            http
                    .securityMatcher("/webauthn/**", "/login/webauthn")
                    //bez tego preflight OPTIONS (np. przed POST /webauthn/register z Authorization
                    //header z innego originu) wpada w anyRequest().authenticated() i dostaje 401
                    //ZANIM globalny CorsFilter (siedzi za FilterChainProxy) zdazy dopisac naglowki
                    //CORS - przegladarka blokuje to jako blad CORS, mimo ze to w istocie 401.
                    //Ten sam wzorzec co w SecurityConfig (glowny lancuch).
                    .cors(Customizer.withDefaults())
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(IF_REQUIRED))
                    .authorizeHttpRequests(req -> req
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            //logowanie passkeyem - publiczne, alternatywa dla /api/v1/auth/login
                            .requestMatchers(HttpMethod.POST, "/webauthn/authenticate/options", "/login/webauthn").permitAll()
                            //rejestracja/usuwanie klucza i wymiana sesji na JWT (/webauthn/token) -
                            //tylko dla juz zalogowanego (JWT z jwtAuthFilter ponizej)
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .accessDeniedHandler(jwtAccessDeniedHandler)
                            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    )
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .webAuthn(webAuthn -> webAuthn
                            .rpId(rpId)
                            .rpName(rpName)
                            .allowedOrigins(allowedOrigins.toArray(new String[0]))
                            //mamy wlasny frontend - nie potrzebujemy wbudowanej strony testowej Spring Security
                            .disableDefaultRegistrationPage(true)
                    );

            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("Nie udalo sie skonfigurowac WebAuthn SecurityFilterChain", e);
        }
    }
}
