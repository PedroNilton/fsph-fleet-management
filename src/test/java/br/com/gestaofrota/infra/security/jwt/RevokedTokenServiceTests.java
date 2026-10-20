package br.com.gestaofrota.infra.security.jwt;

import br.com.gestaofrota.infra.repositories.RevokedTokenRepository;
import br.com.gestaofrota.infra.security.services.RevokedTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokedTokenServiceTests {

    @Mock
    private RevokedTokenRepository repository;

    @Mock
    private JwtService jwtService;

    @Test
    void storesOnlyTokenHashAndRejectsTheSameToken() {
        String token = "jwt-assinado";
        Instant expiration = Instant.parse("2026-10-16T12:00:00Z");
        when(jwtService.getExpiration(token)).thenReturn(expiration);
        RevokedTokenService service = new RevokedTokenService(repository, jwtService);

        service.revoke(token);

        ArgumentCaptor<String> savedHash = ArgumentCaptor.forClass(String.class);
        verify(repository).insertIfAbsent(savedHash.capture(), org.mockito.ArgumentMatchers.eq(expiration));
        assertThat(savedHash.getValue()).hasSize(64).isNotEqualTo(token);

        when(repository.existsByTokenHash(savedHash.getValue())).thenReturn(true);
        assertThat(service.isRevoked(token)).isTrue();
    }
}
