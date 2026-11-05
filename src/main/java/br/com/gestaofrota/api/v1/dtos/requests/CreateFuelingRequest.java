package br.com.gestaofrota.api.v1.dtos.requests;

import br.com.gestaofrota.domain.enums.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateFuelingRequest(
        @NotNull UUID vehicleId,
        @NotNull UUID stationId,
        @NotNull LocalDate fueledOn,
        @NotNull @PositiveOrZero Long odometerKm,
        @NotNull @DecimalMin(value = "0.001") BigDecimal liters,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalValue,
        @NotNull FuelType fuelType
) {
}

