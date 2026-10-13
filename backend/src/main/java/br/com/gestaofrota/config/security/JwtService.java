package br.com.gestaofrota.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC256(properties.secret());
        this.verifier = JWT.require(algorithm)
                .withIssuer(properties.issuer())
                .build();
    }

    public String generateToken(UserDetails user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.expiration());

        return JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(user.getUsername())
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

    private DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
