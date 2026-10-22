package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.models.DriverLicense;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DriverLicenseResponse(
        UUID id, UUID driverId,
        String licenseNumber,
        String category,
        LocalDate issuedOn,
        LocalDate expiresOn,
        RecordStatus status,
        boolean expired,
        boolean expiringWithin30Days,
        Instant createdAt
) {
    public static DriverLicenseResponse from(DriverLicense license) {
        LocalDate today = LocalDate.now();
        boolean active = license.getStatus() == RecordStatus.ACTIVE;

        return new DriverLicenseResponse(
                license.getId(),
                license.getDriver().getId(),
                license.getLicenseNumber(),
                license.getCategory(),
                license.getIssuedOn(),
                license.getExpiresOn(),
                license.getStatus(),
                active && license.getExpiresOn().isBefore(today),
                active && !license.getExpiresOn().isBefore(today) && !license.getExpiresOn().isAfter(today.plusDays(30)),
                license.getCreatedAt()
        );
    }
}