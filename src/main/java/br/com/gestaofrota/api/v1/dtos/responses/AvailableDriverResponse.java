package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.DriverLicense;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;

import java.time.LocalDate;
import java.util.UUID;

public record AvailableDriverResponse(
        UUID driverId,
        UUID userId,
        String name,
        String email,
        UUID sectorId,
        String sectorName,
        String licenseNumber,
        String licenseCategory,
        LocalDate licenseExpiresOn
) {
    public static AvailableDriverResponse from(Driver driver, DriverLicense license) {
        User user = driver.getUser();
        Sector sector = user != null ? user.getSector() : null;
        return new AvailableDriverResponse(
                driver.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                sector != null ? sector.getId() : null,
                sector != null ? sector.getName() : null,
                license.getLicenseNumber(),
                license.getCategory(),
                license.getExpiresOn()
        );
    }
}
