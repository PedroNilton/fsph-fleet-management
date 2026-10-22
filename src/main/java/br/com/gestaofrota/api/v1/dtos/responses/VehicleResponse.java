
package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.VehicleStatus;
import br.com.gestaofrota.domain.models.Vehicle;

import java.util.UUID;

public record VehicleResponse(
        UUID id,
        UUID sectorId,
        String plate,
        String model,
        int capacity,
        int currentMileage,
        VehicleStatus status
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getSector().getId(),
                vehicle.getPlate(),
                vehicle.getModel(),
                vehicle.getCapacity(),
                vehicle.getCurrent_mileage(),
                vehicle.getStatus()
        );
    }
}
