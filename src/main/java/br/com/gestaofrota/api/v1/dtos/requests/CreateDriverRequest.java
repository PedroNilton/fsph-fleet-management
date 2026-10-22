package br.com.gestaofrota.api.v1.dtos.requests;

import br.com.gestaofrota.domain.enums.DriverStatus;
import br.com.gestaofrota.domain.models.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDriverRequest(
        @NotNull User user,
        @NotBlank String license_number,
        @NotNull DriverStatus status
) {
}
