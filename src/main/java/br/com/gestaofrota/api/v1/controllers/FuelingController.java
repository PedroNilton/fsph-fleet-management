package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.requests.CreateFuelingRequest;
import br.com.gestaofrota.api.v1.dtos.responses.FleetConsumptionReportResponse;
import br.com.gestaofrota.api.v1.dtos.responses.FuelingResponse;
import br.com.gestaofrota.api.v1.dtos.responses.VehicleConsumptionSummaryResponse;
import br.com.gestaofrota.application.services.FuelingService;
import br.com.gestaofrota.domain.enums.FuelType;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FuelingController {

    private final FuelingService fuelings;

    public FuelingController(FuelingService fuelings) {
        this.fuelings = fuelings;
    }

    @PostMapping("/fuelings")
    @PreAuthorize("hasAuthority('FUELINGS_WRITE')")
    public ResponseEntity<FuelingResponse> register(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateFuelingRequest request
    ) {
        FuelingResponse result = FuelingResponse.from(
                fuelings.register(
                        user.id(),
                        request.vehicleId(),
                        request.stationId(),
                        request.fueledOn(),
                        request.odometerKm(),
                        request.liters(),
                        request.totalValue(),
                        request.fuelType()
                )
        );
        return ResponseEntity.created(URI.create("/api/v1/fuelings/" + result.id())).body(result);
    }

    @GetMapping("/fuelings/mine")
    @PreAuthorize("hasAnyAuthority('FUELINGS_WRITE', 'FUELINGS_READ')")
    public List<FuelingResponse> listMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return fuelings.listMine(user.id()).stream()
                .map(FuelingResponse::from)
                .toList();
    }

    @GetMapping("/fuelings")
    @PreAuthorize("hasAuthority('FUELINGS_READ')")
    public List<FuelingResponse> list(
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID stationId,
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Boolean outOfPatternOnly
    ) {
        return fuelings.list(vehicleId, stationId, fuelType, outOfPatternOnly).stream()
                .map(FuelingResponse::from)
                .toList();
    }

    @GetMapping("/fuelings/alerts")
    @PreAuthorize("hasAuthority('FUELINGS_READ')")
    public List<FuelingResponse> listAlerts(@RequestParam(required = false) UUID vehicleId) {
        return fuelings.listAnomalyAlerts(vehicleId).stream()
                .map(FuelingResponse::from)
                .toList();
    }

    @GetMapping("/fuelings/consumption")
    @PreAuthorize("hasAuthority('FUELINGS_READ')")
    public FleetConsumptionReportResponse compareFleetConsumption() {
        return fuelings.compareFleetConsumption();
    }

    @GetMapping("/fuelings/{id}")
    @PreAuthorize("hasAuthority('FUELINGS_READ')")
    public FuelingResponse find(@PathVariable UUID id) {
        return FuelingResponse.from(fuelings.find(id));
    }

    @GetMapping("/vehicles/{vehicleId}/fuelings")
    @PreAuthorize("hasAuthority('FUELINGS_READ')")
    public List<FuelingResponse> listByVehicle(@PathVariable UUID vehicleId) {
        return fuelings.listByVehicle(vehicleId).stream()
                .map(FuelingResponse::from)
                .toList();
    }

    @GetMapping("/vehicles/{vehicleId}/fuelings/consumption")
    @PreAuthorize("hasAuthority('FUELINGS_READ')")
    public VehicleConsumptionSummaryResponse summarizeByVehicle(@PathVariable UUID vehicleId) {
        return fuelings.summarizeVehicleConsumption(vehicleId);
    }
}

