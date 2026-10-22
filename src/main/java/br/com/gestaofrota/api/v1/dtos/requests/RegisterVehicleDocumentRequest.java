package br.com.gestaofrota.api.v1.dtos.requests;

import br.com.gestaofrota.domain.enums.VehicleDocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterVehicleDocumentRequest(
        @NotNull VehicleDocumentType type,
        @Size(max = 100) String documentNumber,
        LocalDate issuedOn,
        @NotNull LocalDate expiresOn
) {
}