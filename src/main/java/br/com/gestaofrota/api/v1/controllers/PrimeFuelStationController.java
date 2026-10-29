package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.requests.CreatePrimeFuelStationRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdatePrimeFuelStationRequest;
import br.com.gestaofrota.api.v1.dtos.responses.PrimeFuelStationResponse;
import br.com.gestaofrota.application.services.PrimeFuelStationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fuel-stations")
public class PrimeFuelStationController {

    private final PrimeFuelStationService stations;

    public PrimeFuelStationController(PrimeFuelStationService stations) {
        this.stations = stations;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FUEL_STATIONS_READ')")
    public List<PrimeFuelStationResponse> list(@RequestParam(required = false) Boolean activeOnly) {
        return stations.list(activeOnly).stream()
                .map(PrimeFuelStationResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FUEL_STATIONS_READ')")
    public PrimeFuelStationResponse find(@PathVariable UUID id) {
        return PrimeFuelStationResponse.from(stations.find(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FUEL_STATIONS_WRITE')")
    public ResponseEntity<PrimeFuelStationResponse> create(@Valid @RequestBody CreatePrimeFuelStationRequest request) {
        PrimeFuelStationResponse result = PrimeFuelStationResponse.from(
                stations.create(
                        request.name(),
                        request.cnpj(),
                        request.city(),
                        request.state(),
                        request.address()
                )
        );
        return ResponseEntity.created(URI.create("/api/v1/fuel-stations/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FUEL_STATIONS_WRITE')")
    public PrimeFuelStationResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePrimeFuelStationRequest request
    ) {
        return PrimeFuelStationResponse.from(
                stations.update(
                        id,
                        request.name(),
                        request.cnpj(),
                        request.city(),
                        request.state(),
                        request.address()
                )
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FUEL_STATIONS_WRITE')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        stations.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('FUEL_STATIONS_WRITE')")
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        stations.reactivate(id);
        return ResponseEntity.noContent().build();
    }
}

