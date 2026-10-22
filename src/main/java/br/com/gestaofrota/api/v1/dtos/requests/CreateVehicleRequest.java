package br.com.gestaofrota.api.v1.dtos.requests;


import br.com.gestaofrota.domain.enums.VehicleStatus;
import br.com.gestaofrota.domain.models.Sector;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateVehicleRequest(
        @NotNull UUID sectorId,
        @NotBlank String plate,
        @NotBlank String model,
        @NotNull @Min(1) Integer capacity,
        @NotNull @Min(0) Integer currentMileage,
        @NotNull VehicleStatus status
) {}