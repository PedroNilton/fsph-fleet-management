package br.com.gestaofrota.infra.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC256(properties.secret());
        this.verifier = JWT.require(algorithm).withIssuer(properties.issuer()).build();
    }

    public String generateToken(UserDetails user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.expiration());

        return JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(user.getUsername())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public String extractSubject(String token) {
        return verify(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails user) {
        return extractSubject(token).equalsIgnoreCase(user.getUsername()) && user.isEnabled();
    }

    public long getExpirationSeconds() {
        return properties.expiration().toSeconds();
    }

    public Instant getExpiration(String token) {
        return verify(token).getExpiresAt().toInstant();
    }

    private DecodedJWT verify(String token) {
        DecodedJWT decoded = verifier.verify(token);

        if (decoded.getExpiresAt() == null) {
            throw new JWTVerificationException("Token sem expiração");
        }

        return decoded;
    }
}
