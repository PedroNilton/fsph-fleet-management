package br.com.gestaofrota.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres";

    @Test
    void generatesSignedTokenForAuthenticatedUser() {
        JwtService service = new JwtService(
                new JwtProperties(SECRET, Duration.ofHours(1), "gestao-frotas-test")
        );
        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername("admin@exemplo.com")
                .password("senha-codificada")
                .roles("ADMIN")
                .build();

        String token = service.generateToken(user);

        assertThat(service.extractSubject(token)).isEqualTo("admin@exemplo.com");
        assertThat(service.isTokenValid(token, user)).isTrue();
        assertThat(service.getExpirationSeconds()).isEqualTo(3600L);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService issuer = new JwtService(
                new JwtProperties(SECRET, Duration.ofHours(1), "gestao-frotas-test")
        );
        JwtService validator = new JwtService(
                new JwtProperties(
                        "outro-segredo-de-teste-com-mais-de-32-caracteres",
                        Duration.ofHours(1),
                        "gestao-frotas-test"
                )
        );
        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername("admin@exemplo.com")
                .password("senha-codificada")
                .roles("ADMIN")
                .build();

        String token = issuer.generateToken(user);

        assertThatThrownBy(() -> validator.extractSubject(token))
                .isInstanceOf(JWTVerificationException.class);
    }
}
