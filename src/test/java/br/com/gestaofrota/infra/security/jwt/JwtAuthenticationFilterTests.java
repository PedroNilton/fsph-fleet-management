package br.com.gestaofrota.infra.security.jwt;

import br.com.gestaofrota.infra.security.services.CustomUserDetailsService;
import br.com.gestaofrota.infra.security.services.RevokedTokenService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private RevokedTokenService revokedTokenService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsInvalidTokenBeforeLoadingUser() throws Exception {
        MockHttpServletRequest request = requestWith("jwt-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractSubject("jwt-invalido"))
                .thenThrow(new JWTVerificationException("Token inválido"));

        filter().doFilterInternal(request, response, filterChain);

        verify(authenticationEntryPoint).commence(eq(request), eq(response), any());
        verifyNoInteractions(userDetailsService);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsTokenIssuedBeforeUserWasDeactivated() throws Exception {
        MockHttpServletRequest request = requestWith("jwt-ativo-na-emissao");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractSubject("jwt-ativo-na-emissao")).thenReturn("usuario@exemplo.com");
        when(userDetailsService.loadUserByUsername("usuario@exemplo.com"))
                .thenThrow(new UsernameNotFoundException("Credenciais inválidas"));

        filter().doFilterInternal(request, response, filterChain);

        verify(authenticationEntryPoint).commence(eq(request), eq(response), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsRevokedTokenBeforeLoadingUser() throws Exception {
        MockHttpServletRequest request = requestWith("jwt-revogado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractSubject("jwt-revogado")).thenReturn("usuario@exemplo.com");
        when(revokedTokenService.isRevoked("jwt-revogado")).thenReturn(true);

        filter().doFilterInternal(request, response, filterChain);

        verify(authenticationEntryPoint).commence(eq(request), eq(response), any());
        verifyNoInteractions(userDetailsService);
        verify(filterChain, never()).doFilter(any(), any());
    }

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(
                jwtService,
                revokedTokenService,
                userDetailsService,
                authenticationEntryPoint
        );
    }

    private MockHttpServletRequest requestWith(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
