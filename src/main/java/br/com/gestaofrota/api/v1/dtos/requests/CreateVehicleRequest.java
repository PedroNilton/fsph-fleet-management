package br.com.gestaofrota.api.v1.dtos.requests;

import br.com.gestaofrota.domain.enums.VehicleOwner;
import br.com.gestaofrota.domain.enums.VehicleRole;
import br.com.gestaofrota.domain.enums.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateVehicleRequest(
        @NotBlank @Size(max = 150) String plate,
        @NotBlank @Size(max = 150) String model,
        @NotNull VehicleRole role,
        @NotNull VehicleOwner owner,
        @NotNull VehicleStatus status,
        @NotNull Integer availableSeats
) {
}
