package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterDriverLicenseRequest(
        @NotBlank @Size(max = 30) String licenseNumber,
        @NotBlank @Size(max = 10) String category,
        @NotNull LocalDate issuedOn,
        @NotNull LocalDate expiresOn
) {
}