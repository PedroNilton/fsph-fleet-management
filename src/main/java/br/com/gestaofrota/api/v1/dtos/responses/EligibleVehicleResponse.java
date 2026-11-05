package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.Vehicle;

import java.util.UUID;

public record EligibleVehicleResponse(
        UUID id,
        UUID sectorId,
        String sectorName,
        String plate,
        String model,
        int totalCapacity,
        int availablePassengerSeats,
        String usageType,
        long currentOdometerKm
) {
    public static EligibleVehicleResponse from(Vehicle vehicle) {
        Sector sector = vehicle.getSector();
        return new EligibleVehicleResponse(
                vehicle.getId(),
                sector != null ? sector.getId() : null,
                sector != null ? sector.getName() : null,
                vehicle.getPlate(),
                vehicle.getModel(),
                vehicle.getCapacity(),
                vehicle.getAvailablePassengerSeats(),
                vehicle.getUsageType(),
                vehicle.getCurrentOdometerKm()
        );
    }
}
