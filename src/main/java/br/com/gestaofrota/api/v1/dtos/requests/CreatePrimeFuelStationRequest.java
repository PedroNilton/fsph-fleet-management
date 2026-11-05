package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePrimeFuelStationRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 18) String cnpj,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotBlank @Size(max = 255) String address
) {
}

