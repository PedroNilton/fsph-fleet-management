package br.com.gestaofrota.modules.user.bootstrap;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String sectorName
) {
}
