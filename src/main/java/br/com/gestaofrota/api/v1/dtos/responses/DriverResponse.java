package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.models.Driver;
import java.time.Instant;
import java.util.UUID;

public record DriverResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        UUID sectorId,
        boolean active,
        boolean userActive,
        Instant createdAt
) {
    public static DriverResponse from(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getUser().getId(),
                driver.getUser().getName(),
                driver.getUser().getEmail(),
                driver.getUser().getSector().getId(),
                driver.isActive(),
                driver.getUser().isActive(),
                driver.getCreatedAt()
        );
    }
}