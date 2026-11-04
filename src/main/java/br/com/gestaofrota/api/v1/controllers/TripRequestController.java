package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.requests.CreateTripRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdateTripRequest;
import br.com.gestaofrota.api.v1.dtos.responses.EligibleVehicleResponse;
import br.com.gestaofrota.api.v1.dtos.responses.TripRequestResponse;
import br.com.gestaofrota.application.services.TripRequestService;
import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trip-requests")
public class TripRequestController {

    private final TripRequestService tripRequests;

    public TripRequestController(TripRequestService tripRequests) {
        this.tripRequests = tripRequests;
    }

    @GetMapping("/eligible-vehicles")
    @PreAuthorize("hasAnyAuthority('TRIP_REQUESTS_WRITE', 'TRIP_REQUESTS_MANAGE')")
    public List<EligibleVehicleResponse> listEligibleVehicles(
            @RequestParam int passengers,
            @RequestParam(required = false) Instant departureAt,
            @RequestParam(required = false) Instant expectedReturnAt
    ) {
        return tripRequests.listEligibleVehicles(passengers, departureAt, expectedReturnAt).stream()
                .map(EligibleVehicleResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRIP_REQUESTS_WRITE')")
    public ResponseEntity<TripRequestResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateTripRequest request
    ) {
        TripRequestResponse result = TripRequestResponse.from(
                tripRequests.create(
                        user.id(),
                        request.departureAt(),
                        request.expectedReturnAt(),
                        request.origin(),
                        request.destination(),
                        request.purpose(),
                        request.passengerCount(),
                        request.requestedVehicleId()
                )
        );
        return ResponseEntity.created(URI.create("/api/v1/trip-requests/" + result.id())).body(result);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('TRIP_REQUESTS_WRITE')")
    public List<TripRequestResponse> listMine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) TripRequestStatus status
    ) {
        return tripRequests.listMine(user.id(), status).stream()
                .map(TripRequestResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TRIP_REQUESTS_WRITE', 'TRIP_REQUESTS_READ')")
    public TripRequestResponse findById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        boolean canReadAll = user.permissions().contains(Permission.TRIP_REQUESTS_READ);
        return TripRequestResponse.from(tripRequests.findVisibleTo(id, user.id(), canReadAll));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_REQUESTS_WRITE')")
    public TripRequestResponse updatePending(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripRequest request
    ) {
        return TripRequestResponse.from(
                tripRequests.updatePending(
                        id,
                        user.id(),
                        request.departureAt(),
                        request.expectedReturnAt(),
                        request.origin(),
                        request.destination(),
                        request.purpose(),
                        request.passengerCount(),
                        request.requestedVehicleId()
                )
        );
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('TRIP_REQUESTS_WRITE')")
    public TripRequestResponse cancelPending(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        return TripRequestResponse.from(tripRequests.cancelByRequester(id, user.id()));
    }
}
