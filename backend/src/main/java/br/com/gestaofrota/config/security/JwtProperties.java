package br.com.gestaofrota.config.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(@NotBlank @Size(min = 32) String secret, @NotNull Duration expiration, @NotBlank String issuer) {
    public JwtProperties {
        if (expiration != null && (expiration.isZero() || expiration.isNegative())) {
            throw new IllegalArgumentException("A expiração do JWT deve ser positiva");
        }
    }
}
