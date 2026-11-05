package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;

import java.time.Instant;
import java.util.UUID;

public record TripRequestResponse(
        UUID id,
        UUID requesterId,
        String requesterName,
        UUID requesterSectorId,
        String requesterSectorName,
        Instant departureAt,
        Instant expectedReturnAt,
        String origin,
        String destination,
        String purpose,
        int passengerCount,
        UUID requestedVehicleId,
        String requestedVehiclePlate,
        String requestedVehicleModel,
        TripRequestStatus status,
        UUID assignedVehicleId,
        String assignedVehiclePlate,
        String assignedVehicleModel,
        UUID assignedDriverId,
        String assignedDriverName,
        UUID decidedByUserId,
        String decidedByName,
        Instant decidedAt,
        String rejectionReason,
        String decisionNotes,
        UUID scheduledByUserId,
        String scheduledByName,
        Instant scheduledAt,
        Instant canceledAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static TripRequestResponse from(TripRequest request) {
        User requester = request.getRequester();
        Sector requesterSector = requester != null ? requester.getSector() : null;
        Vehicle requestedVehicle = request.getRequestedVehicle();
        Vehicle assignedVehicle = request.getAssignedVehicle();
        Driver assignedDriver = request.getAssignedDriver();
        User decidedBy = request.getDecidedBy();
        User scheduledBy = request.getScheduledBy();

        return new TripRequestResponse(
                request.getId(),
                requester != null ? requester.getId() : null,
                requester != null ? requester.getName() : null,
                requesterSector != null ? requesterSector.getId() : null,
                requesterSector != null ? requesterSector.getName() : null,
                request.getDepartureAt(),
                request.getExpectedReturnAt(),
                request.getOrigin(),
                request.getDestination(),
                request.getPurpose(),
                request.getPassengerCount(),
                requestedVehicle != null ? requestedVehicle.getId() : null,
                requestedVehicle != null ? requestedVehicle.getPlate() : null,
                requestedVehicle != null ? requestedVehicle.getModel() : null,
                request.getStatus(),
                assignedVehicle != null ? assignedVehicle.getId() : null,
                assignedVehicle != null ? assignedVehicle.getPlate() : null,
                assignedVehicle != null ? assignedVehicle.getModel() : null,
                assignedDriver != null ? assignedDriver.getId() : null,
                assignedDriver != null && assignedDriver.getUser() != null ? assignedDriver.getUser().getName() : null,
                decidedBy != null ? decidedBy.getId() : null,
                decidedBy != null ? decidedBy.getName() : null,
                request.getDecidedAt(),
                request.getRejectionReason(),
                request.getDecisionNotes(),
                scheduledBy != null ? scheduledBy.getId() : null,
                scheduledBy != null ? scheduledBy.getName() : null,
                request.getScheduledAt(),
                request.getCanceledAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
