package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserPasswordRequest(
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
