package br.com.gestaofrota.infra.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotEmpty List<@NotBlank String> allowedOrigins
) {
}
