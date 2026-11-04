package br.com.gestaofrota.api.v1.dtos.responses;

import java.time.Instant;
import java.util.List;

public record FleetCalendarResponse(
        Instant from,
        Instant to,
        int totalActiveVehicles,
        int vehiclesInMaintenance,
        int vehiclesWithReservations,
        int vehiclesCompletelyFree,
        List<FleetCalendarReservationResponse> reservations,
        List<VehicleCalendarAvailabilityResponse> vehicles
) {
}
