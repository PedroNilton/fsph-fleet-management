package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.requests.CreateDriverRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdateSectorRequest;
import br.com.gestaofrota.api.v1.dtos.responses.DriverResponse;
import br.com.gestaofrota.api.v1.dtos.responses.SectorResponse;
import br.com.gestaofrota.application.services.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverController {
    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }
    @GetMapping
    public List<DriverResponse> list() {
            return driverService.list().stream().map(DriverResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DriverResponse find(@PathVariable UUID id){
        return DriverResponse.from(driverService.findById(id));
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(@RequestBody CreateDriverRequest request){
        DriverResponse response = DriverResponse.from(driverService.create(request.user(),request.license_number(),request.status()));
        return ResponseEntity.created(URI.create("/api/v1/driver/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public DriverResponse update(@PathVariable UUID id, @RequestBody CreateDriverRequest request) {
        return DriverResponse.from(driverService.update(id, request.user(),request.license_number(),request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
