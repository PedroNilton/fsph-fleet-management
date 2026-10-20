package br.com.gestaofrota.api.v1.dtos.requests;

import br.com.gestaofrota.domain.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleStatus(
        @NotNull VehicleStatus status
) {

}
