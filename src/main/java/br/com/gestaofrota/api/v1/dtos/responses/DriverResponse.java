package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.DriverStatus;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.User;

import java.util.UUID;

public record DriverResponse(
        UUID id,
        User user,
        String license_number,
        DriverStatus status
) {
    public static  DriverResponse from(Driver driver){
        return new DriverResponse(
            driver.getId(),
            driver.getUser(),
            driver.getLicense_number(),
            driver.getStatus()
        );
    }
}
