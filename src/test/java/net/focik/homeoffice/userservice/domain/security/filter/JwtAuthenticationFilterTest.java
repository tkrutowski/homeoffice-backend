package net.focik.homeoffice.userservice.domain.security.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import net.focik.homeoffice.userservice.domain.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    @DisplayName("should continue unauthenticated instead of failing with 500 when the JWT is expired")
    void doFilter_ShouldContinueUnauthenticated_WhenTokenExpired() throws Exception {
        MockHttpServletRequest request = requestWithBearer("expired");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername(anyString())).thenThrow(new ExpiredJwtException(null, null, "expired"));

        assertThatCode(() -> new JwtAuthenticationFilter(jwtService, userDetailsService)
                .doFilter(request, response, filterChain)).doesNotThrowAnyException();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should continue unauthenticated when the JWT is malformed")
    void doFilter_ShouldContinueUnauthenticated_WhenTokenMalformed() throws Exception {
        MockHttpServletRequest request = requestWithBearer("garbage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername(anyString())).thenThrow(new MalformedJwtException("bad"));

        new JwtAuthenticationFilter(jwtService, userDetailsService).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
