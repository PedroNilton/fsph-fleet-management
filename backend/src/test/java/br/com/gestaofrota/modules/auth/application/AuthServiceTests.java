package br.com.gestaofrota.modules.auth.application;

import br.com.gestaofrota.config.security.JwtService;
import br.com.gestaofrota.modules.auth.api.dto.LoginRequest;
import br.com.gestaofrota.modules.auth.api.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Test
    void authenticatesNormalizedEmailBeforeGeneratingToken() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("admin@exemplo.com")
                .password("senha-codificada")
                .roles("ADMIN")
                .build();
        Authentication authenticated = mock(Authentication.class);
        when(authenticated.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticated);
        when(jwtService.generateToken(principal)).thenReturn("jwt-assinado");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = new AuthService(authenticationManager, jwtService)
                .login(new LoginRequest(" ADMIN@EXEMPLO.COM ", "senha"));

        ArgumentCaptor<Authentication> authenticationCaptor =
                ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        UsernamePasswordAuthenticationToken credentials =
                (UsernamePasswordAuthenticationToken) authenticationCaptor.getValue();
        assertThat(credentials.getPrincipal()).isEqualTo("admin@exemplo.com");
        assertThat(credentials.getCredentials()).isEqualTo("senha");
        assertThat(response.accessToken()).isEqualTo("jwt-assinado");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        verify(jwtService).generateToken(principal);
    }
}
