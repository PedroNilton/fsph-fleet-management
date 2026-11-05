package br.com.gestaofrota.api.v1.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleConsumptionSummaryResponse(
        UUID vehicleId,
        String vehiclePlate,
        String vehicleModel,
        String usageType,
        long currentOdometerKm,
        int totalFuelings,
        BigDecimal totalLiters,
        BigDecimal totalSpent,
        long totalMeasuredDistanceKm,
        BigDecimal averageKmPerLiter,
        BigDecimal fleetAverageKmPerLiter,
        BigDecimal deviationFromFleetPercent,
        int anomalyCount,
        LocalDate lastFueledOn
) {
}

