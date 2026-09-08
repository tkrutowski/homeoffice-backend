package net.focik.homeoffice.userservice.domain.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autoryzuje żądania do /internal/** na podstawie nagłówka X-Internal-Token zamiast JWT -
 * to wywołania z workflow n8n (webhook ingest kredytów), nie od użytkownika. Odpowiada
 * JwtAuthenticationFilter, tylko dla innej klasy wywołujących.
 * <p>
 * To druga warstwa ochrony obok samego nginx.conf, który i tak nie przepuszcza /internal/**
 * z internetu (mapowane jest tylko /api/** i /actuator/**).
 */
@Slf4j
@Component
public class InternalTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Token";
    static final String ROLE_INTERNAL = "ROLE_INTERNAL";

    @Value("${internal.api.token:}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedToken = request.getHeader(HEADER);

        if (expectedToken != null && !expectedToken.isBlank()
                && providedToken != null && providedToken.equals(expectedToken)) {
            var authToken = new UsernamePasswordAuthenticationToken(
                    "n8n", null, List.of(new SimpleGrantedAuthority(ROLE_INTERNAL)));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
            log.warn("Rejected request to {}: missing or invalid {} header", request.getRequestURI(), HEADER);
        }

        filterChain.doFilter(request, response);
    }
}
