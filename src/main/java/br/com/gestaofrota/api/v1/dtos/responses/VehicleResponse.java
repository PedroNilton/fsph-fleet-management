package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.VehicleOwner;
import br.com.gestaofrota.domain.enums.VehicleRole;
import br.com.gestaofrota.domain.enums.VehicleStatus;
import br.com.gestaofrota.domain.models.Vehicle;

public record VehicleResponse(
        String plate,
        String model,
        VehicleRole role,
        VehicleOwner owner,
        VehicleStatus status,
        Integer availableSeats) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getPlate(),
                vehicle.getModel(),
                vehicle.getRole(),
                vehicle.getOwner(),
                vehicle.getStatus(),
                vehicle.getAvailableSeats()

        );
    }
}
