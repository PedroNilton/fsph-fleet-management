package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.FuelType;
import br.com.gestaofrota.domain.models.Fueling;
import br.com.gestaofrota.domain.models.PrimeFuelStation;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FuelingResponse(
        UUID id,
        UUID vehicleId,
        String vehiclePlate,
        String vehicleModel,
        UUID stationId,
        String stationName,
        String stationCnpj,
        UUID registeredByUserId,
        String registeredByName,
        LocalDate fueledOn,
        long odometerKm,
        Long previousOdometerKm,
        Long distanceDrivenKm,
        BigDecimal liters,
        BigDecimal totalValue,
        BigDecimal pricePerLiter,
        FuelType fuelType,
        BigDecimal consumptionKmPerLiter,
        BigDecimal referenceAverageKmPerLiter,
        boolean outOfPattern,
        String anomalyReason,
        Instant createdAt
) {
    public static FuelingResponse from(Fueling fueling) {
        Vehicle vehicle = fueling.getVehicle();
        PrimeFuelStation station = fueling.getStation();
        User registeredBy = fueling.getRegisteredBy();
        BigDecimal pricePerLiter = fueling.getTotalValue()
                .divide(fueling.getLiters(), 2, RoundingMode.HALF_UP);

        return new FuelingResponse(
                fueling.getId(),
                vehicle.getId(),
                vehicle.getPlate(),
                vehicle.getModel(),
                station.getId(),
                station.getName(),
                station.getCnpj(),
                registeredBy.getId(),
                registeredBy.getName(),
                fueling.getFueledOn(),
                fueling.getOdometerKm(),
                fueling.getPreviousOdometerKm(),
                fueling.getDistanceDrivenKm(),
                fueling.getLiters(),
                fueling.getTotalValue(),
                pricePerLiter,
                fueling.getFuelType(),
                fueling.getConsumptionKmPerLiter(),
                fueling.getReferenceAverageKmPerLiter(),
                fueling.isOutOfPattern(),
                fueling.getAnomalyReason(),
                fueling.getCreatedAt()
        );
    }
}

