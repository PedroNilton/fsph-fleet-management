package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.enums.VehicleDocumentType;
import br.com.gestaofrota.domain.models.VehicleDocument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleDocumentResponse(
        UUID id, UUID vehicleId,
        VehicleDocumentType type,
        String documentNumber,
        LocalDate issuedOn,
        LocalDate expiresOn,
        RecordStatus status,
        boolean expired,
        boolean expiringWithin30Days,
        Instant createdAt
) {
    public static VehicleDocumentResponse from(VehicleDocument document) {
        LocalDate today = LocalDate.now();
        boolean active = document.getStatus() == RecordStatus.ACTIVE;

        return new VehicleDocumentResponse(
                document.getId(),
                document.getVehicle().getId(),
                document.getType(),
                document.getDocumentNumber(),
                document.getIssuedOn(),
                document.getExpiresOn(),
                document.getStatus(),
                active && document.getExpiresOn().isBefore(today),
                active && !document.getExpiresOn().isBefore(today) && !document.getExpiresOn().isAfter(today.plusDays(30)),
                document.getCreatedAt()
        );
    }
}