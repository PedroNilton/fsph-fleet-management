package br.com.gestaofrota.api.v1.dtos.responses;

import java.util.List;
import java.util.UUID;

public record VehicleCalendarAvailabilityResponse(
        UUID vehicleId,
        String plate,
        String model,
        int totalCapacity,
        int availablePassengerSeats,
        boolean active,
        boolean inMaintenance,
        boolean completelyFreeInPeriod,
        List<FleetCalendarReservationResponse> reservations
) {
}
