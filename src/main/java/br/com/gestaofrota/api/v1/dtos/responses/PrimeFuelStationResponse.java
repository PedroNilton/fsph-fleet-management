package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.models.PrimeFuelStation;

import java.time.Instant;
import java.util.UUID;

public record PrimeFuelStationResponse(
        UUID id,
        String name,
        String cnpj,
        String city,
        String state,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static PrimeFuelStationResponse from(PrimeFuelStation station) {
        return new PrimeFuelStationResponse(
                station.getId(),
                station.getName(),
                station.getCnpj(),
                station.getCity(),
                station.getState(),
                station.getAddress(),
                station.isActive(),
                station.getCreatedAt(),
                station.getUpdatedAt()
        );
    }
}

