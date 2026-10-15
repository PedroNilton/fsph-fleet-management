package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.requests.CreateSectorRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdateSectorRequest;
import br.com.gestaofrota.api.v1.dtos.responses.SectorResponse;
import br.com.gestaofrota.application.services.SectorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sectors")
@PreAuthorize("hasRole('ADMIN')")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    public List<SectorResponse> list() {
        return sectorService.list().stream().map(SectorResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SectorResponse find(@PathVariable UUID id) {
        return SectorResponse.from(sectorService.find(id));
    }

    @PostMapping
    public ResponseEntity<SectorResponse> create(@Valid @RequestBody CreateSectorRequest request) {
        SectorResponse response = SectorResponse.from(sectorService.create(request.name()));
        return ResponseEntity.created(URI.create("/api/v1/sectors/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public SectorResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSectorRequest request) {
        return SectorResponse.from(sectorService.update(id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sectorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
