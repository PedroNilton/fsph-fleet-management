package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.requests.CreateVehicleRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdateVehicleStatus;
import br.com.gestaofrota.api.v1.dtos.responses.VehicleResponse;
import br.com.gestaofrota.domain.enums.VehicleOwner;
import br.com.gestaofrota.domain.enums.VehicleRole;
import br.com.gestaofrota.domain.enums.VehicleStatus;
import br.com.gestaofrota.infra.security.services.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        return vehicleService.create(request);
    }

    @PatchMapping("/{id}/status")
    public VehicleResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleStatus request
    ) {
        return vehicleService.updateStatus(id, request);
    }

    @GetMapping("/{id}")
    public VehicleResponse findById(@PathVariable UUID id) {
        return vehicleService.findById(id);
    }

    @GetMapping("/plate/{plate}")
    public VehicleResponse findByPlate(@PathVariable String plate) {
        return vehicleService.findByPlate(plate);
    }

    @GetMapping
    public List<VehicleResponse> findAll() {
        return vehicleService.findAll();
    }

    @GetMapping("/status/{status}")
    public List<VehicleResponse> findByStatus(
            @PathVariable VehicleStatus status
    ) {
        return vehicleService.findByStatus(status);
    }

    @GetMapping("/owner/{owner}")
    public List<VehicleResponse> findByOwner(
            @PathVariable VehicleOwner owner
    ) {
        return vehicleService.findByOwner(owner);
    }

    @GetMapping("/owner/{owner}/status/{status}")
    public List<VehicleResponse> findByOwnerAndStatus(
            @PathVariable VehicleOwner owner,
            @PathVariable VehicleStatus status
    ) {
        return vehicleService.findByOwnerAndStatus(owner, status);
    }

    @GetMapping("/role/{role}")
    public List<VehicleResponse> findByRole(
            @PathVariable VehicleRole role
    ) {
        return vehicleService.findByRole(role);
    }

    @GetMapping("/available-seats/{availableSeats}")
    public List<VehicleResponse> findByAvailableSeats(
            @PathVariable int availableSeats
    ) {
        return vehicleService.findByAvailableSeats(availableSeats);
    }
}