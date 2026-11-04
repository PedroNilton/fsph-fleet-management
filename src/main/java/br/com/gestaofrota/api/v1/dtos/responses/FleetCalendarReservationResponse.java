package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;

import java.time.Instant;
import java.util.UUID;

public record FleetCalendarReservationResponse(
        UUID tripRequestId,
        TripRequestStatus status,
        Instant departureAt,
        Instant expectedReturnAt,
        String origin,
        String destination,
        String purpose,
        int passengerCount,
        UUID requesterId,
        String requesterName,
        UUID vehicleId,
        String vehiclePlate,
        String vehicleModel,
        UUID driverId,
        String driverName
) {
    public static FleetCalendarReservationResponse from(TripRequest request) {
        User requester = request.getRequester();
        Vehicle vehicle = request.getAssignedVehicle();
        Driver driver = request.getAssignedDriver();
        return new FleetCalendarReservationResponse(
                request.getId(),
                request.getStatus(),
                request.getDepartureAt(),
                request.getExpectedReturnAt(),
                request.getOrigin(),
                request.getDestination(),
                request.getPurpose(),
                request.getPassengerCount(),
                requester != null ? requester.getId() : null,
                requester != null ? requester.getName() : null,
                vehicle != null ? vehicle.getId() : null,
                vehicle != null ? vehicle.getPlate() : null,
                vehicle != null ? vehicle.getModel() : null,
                driver != null ? driver.getId() : null,
                driver != null && driver.getUser() != null ? driver.getUser().getName() : null
        );
    }
}
