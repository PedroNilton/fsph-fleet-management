package br.com.gestaofrota.infra.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTests {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres";

    @Test
    void generatesSignedTokenForAuthenticatedUser() {
        JwtUtil service = new JwtUtil(
                new JwtProperties(SECRET, Duration.ofMinutes(15), "gestao-frotas-test")
        );
        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername("admin@exemplo.com")
                .password("senha-codificada")
                .roles("ADMIN")
                .build();

        String token = service.generateToken(user);

        assertThat(service.extractSubject(token)).isEqualTo("admin@exemplo.com");
        assertThat(service.isTokenValid(token, user)).isTrue();
        assertThat(service.getExpirationSeconds()).isEqualTo(900L);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtUtil issuer = new JwtUtil(
                new JwtProperties(SECRET, Duration.ofMinutes(15), "gestao-frotas-test")
        );
        JwtUtil validator = new JwtUtil(
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

    @Test
    void rejectsExpiredToken() {
        JwtUtil validator = new JwtUtil(
                new JwtProperties(SECRET, Duration.ofMinutes(15), "gestao-frotas-test")
        );
        String expiredToken = JWT.create()
                .withIssuer("gestao-frotas-test")
                .withSubject("admin@exemplo.com")
                .withIssuedAt(Date.from(Instant.now().minusSeconds(120)))
                .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));

        assertThatThrownBy(() -> validator.extractSubject(expiredToken))
                .isInstanceOf(JWTVerificationException.class);
    }
}
