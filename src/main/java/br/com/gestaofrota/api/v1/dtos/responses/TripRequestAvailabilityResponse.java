package br.com.gestaofrota.api.v1.dtos.responses;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TripRequestAvailabilityResponse(
        UUID tripRequestId,
        Instant departureAt,
        Instant expectedReturnAt,
        int passengerCount,
        UUID requestedVehicleId,
        boolean requestedVehicleAvailable,
        List<EligibleVehicleResponse> availableVehicles,
        List<AvailableDriverResponse> availableDrivers
) {
}
