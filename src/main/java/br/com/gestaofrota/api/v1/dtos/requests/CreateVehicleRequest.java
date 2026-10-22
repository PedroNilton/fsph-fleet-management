package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateVehicleRequest(
        UUID sectorId,
        @NotBlank @Size(max = 11) String plate,
        @NotBlank @Size(max = 150) String model,
        @NotNull @Positive Integer capacity,
        @NotBlank @Size(max = 50) String usageType,
        @NotNull @PositiveOrZero Long currentOdometerKm
) {
}