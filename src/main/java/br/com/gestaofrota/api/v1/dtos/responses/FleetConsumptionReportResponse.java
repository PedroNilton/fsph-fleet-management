package br.com.gestaofrota.api.v1.dtos.responses;

import java.math.BigDecimal;
import java.util.List;

public record FleetConsumptionReportResponse(
        BigDecimal fleetAverageKmPerLiter,
        BigDecimal totalFleetLiters,
        BigDecimal totalFleetSpent,
        int totalFuelings,
        int totalAnomalyAlerts,
        List<VehicleConsumptionSummaryResponse> vehicles
) {
}

