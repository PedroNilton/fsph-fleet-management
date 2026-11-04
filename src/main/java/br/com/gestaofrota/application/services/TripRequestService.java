package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.exceptions.UnauthorizedAccessException;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.TripRequestRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TripRequestService {

    private static final Set<TripRequestStatus> ACTIVE_RESERVATION_STATUSES =
            EnumSet.of(TripRequestStatus.APPROVED, TripRequestStatus.SCHEDULED);

    private final TripRequestRepository tripRequests;
    private final UserRepository users;
    private final VehicleRepository vehicles;
    private final MaintenanceAvailabilityGuard maintenanceGuard;

    public TripRequestService(
            TripRequestRepository tripRequests,
            UserRepository users,
            VehicleRepository vehicles,
            MaintenanceAvailabilityGuard maintenanceGuard
    ) {
        this.tripRequests = tripRequests;
        this.users = users;
        this.vehicles = vehicles;
        this.maintenanceGuard = maintenanceGuard;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> listEligibleVehicles(int passengerCount, Instant departureAt, Instant expectedReturnAt) {
        if (passengerCount < 1) {
            throw new InvalidRequestException("O número de passageiros deve ser pelo menos 1");
        }
        if ((departureAt == null) != (expectedReturnAt == null)) {
            throw new InvalidRequestException("Informe tanto a data/hora de saída quanto a prevista de retorno");
        }
        if (departureAt != null && !expectedReturnAt.isAfter(departureAt)) {
            throw new InvalidRequestException("A data/hora prevista de retorno deve ser posterior à saída");
        }

        Set<UUID> reservedVehicleIds = departureAt == null
                ? Set.of()
                : tripRequests.findActiveReservationsOverlapping(ACTIVE_RESERVATION_STATUSES, departureAt, expectedReturnAt)
                .stream()
                .map(TripRequest::getAssignedVehicle)
                .filter(vehicle -> vehicle != null)
                .map(Vehicle::getId)
                .collect(Collectors.toSet());

        return vehicles.findByActiveTrueAndInMaintenanceFalseOrderByPlateAsc().stream()
                .filter(vehicle -> vehicle.supportsPassengers(passengerCount))
                .filter(vehicle -> !reservedVehicleIds.contains(vehicle.getId()))
                .toList();
    }

    @Transactional
    public TripRequest create(
            UUID requesterUserId,
            Instant departureAt,
            Instant expectedReturnAt,
            String origin,
            String destination,
            String purpose,
            int passengerCount,
            UUID requestedVehicleId
    ) {
        User requester = findUser(requesterUserId);
        validatePeriod(departureAt, expectedReturnAt);
        Vehicle requestedVehicle = resolveEligibleRequestedVehicle(
                requestedVehicleId,
                passengerCount,
                departureAt,
                expectedReturnAt,
                null
        );

        TripRequest created = tripRequests.save(new TripRequest(
                requester,
                departureAt,
                expectedReturnAt,
                origin,
                destination,
                purpose,
                passengerCount,
                requestedVehicle
        ));
        return created;
    }

    @Transactional(readOnly = true)
    public List<TripRequest> listMine(UUID requesterUserId, TripRequestStatus status) {
        if (status != null) {
            return tripRequests.findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterUserId, status);
        }
        return tripRequests.findByRequesterIdOrderByCreatedAtDesc(requesterUserId);
    }

    @Transactional(readOnly = true)
    public TripRequest findVisibleTo(UUID requestId, UUID actorUserId, boolean canReadAll) {
        TripRequest request = tripRequests.findWithDetailsById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de viagem", requestId));
        if (!canReadAll && !request.getRequester().getId().equals(actorUserId)) {
            throw new UnauthorizedAccessException();
        }
        return request;
    }

    @Transactional
    public TripRequest updatePending(
            UUID requestId,
            UUID requesterUserId,
            Instant departureAt,
            Instant expectedReturnAt,
            String origin,
            String destination,
            String purpose,
            int passengerCount,
            UUID requestedVehicleId
    ) {
        TripRequest request = lockTripRequest(requestId);
        assertRequesterOwnership(request, requesterUserId);
        validatePeriod(departureAt, expectedReturnAt);

        Vehicle requestedVehicle = resolveEligibleRequestedVehicle(
                requestedVehicleId,
                passengerCount,
                departureAt,
                expectedReturnAt,
                request.getId()
        );

        request.updatePending(
                departureAt,
                expectedReturnAt,
                origin,
                destination,
                purpose,
                passengerCount,
                requestedVehicle
        );
        return request;
    }

    @Transactional
    public TripRequest cancelByRequester(UUID requestId, UUID requesterUserId) {
        TripRequest request = lockTripRequest(requestId);
        assertRequesterOwnership(request, requesterUserId);
        request.cancelByRequester(Instant.now());
        return request;
    }

    private Vehicle resolveEligibleRequestedVehicle(
            UUID requestedVehicleId,
            int passengerCount,
            Instant departureAt,
            Instant expectedReturnAt,
            UUID excludeRequestId
    ) {
        if (requestedVehicleId == null) {
            return null;
        }
        Vehicle vehicle = vehicles.findById(requestedVehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo", requestedVehicleId));
        maintenanceGuard.assertVehicleAvailableForAllocation(requestedVehicleId);
        if (!vehicle.supportsPassengers(passengerCount)) {
            throw new InvalidRequestException(
                    "O veículo selecionado comporta no máximo "
                            + vehicle.getAvailablePassengerSeats()
                            + " passageiro(s), insuficiente para "
                            + passengerCount
                            + " passageiro(s)"
            );
        }
        boolean hasOverlap = tripRequests.findVehicleReservationsOverlapping(
                        requestedVehicleId,
                        ACTIVE_RESERVATION_STATUSES,
                        departureAt,
                        expectedReturnAt
                )
                .stream()
                .anyMatch(existing -> !existing.getId().equals(excludeRequestId));
        if (hasOverlap) {
            throw new ConflictException("O veículo indicado já possui reserva conflitante no período informado");
        }
        return vehicle;
    }

    private static void validatePeriod(Instant departureAt, Instant expectedReturnAt) {
        if (departureAt == null || expectedReturnAt == null) {
            throw new InvalidRequestException("Data/hora de saída e retorno previsto são obrigatórias");
        }
        if (!expectedReturnAt.isAfter(departureAt)) {
            throw new InvalidRequestException("A data/hora prevista de retorno deve ser posterior à saída");
        }
    }

    private static void assertRequesterOwnership(TripRequest request, UUID requesterUserId) {
        if (!request.getRequester().getId().equals(requesterUserId)) {
            throw new UnauthorizedAccessException();
        }
    }

    private User findUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private TripRequest lockTripRequest(UUID requestId) {
        return tripRequests.lockById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de viagem", requestId));
    }
}
