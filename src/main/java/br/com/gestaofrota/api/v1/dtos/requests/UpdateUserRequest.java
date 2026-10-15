package br.com.gestaofrota.api.v1.dtos.requests;

import br.com.gestaofrota.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateUserRequest(
        @NotNull UUID sectorId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(min = 8, max = 72) String password,
        @NotNull UserRole role,
        @NotNull Boolean active
) {
}
