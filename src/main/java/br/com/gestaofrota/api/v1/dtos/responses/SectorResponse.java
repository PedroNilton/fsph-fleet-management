package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.models.Sector;

import java.util.UUID;

public record SectorResponse(UUID id, String name) {

    public static SectorResponse from(Sector sector) {
        return new SectorResponse(sector.getId(), sector.getName());
    }
}
