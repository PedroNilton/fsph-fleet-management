package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.FuelType;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fuelings")
public class Fueling {

    public static final BigDecimal LOW_EFFICIENCY_MULTIPLIER = new BigDecimal("0.75");
    public static final BigDecimal HIGH_EFFICIENCY_MULTIPLIER = new BigDecimal("1.50");
    public static final BigDecimal MIN_PLAUSIBLE_KM_PER_LITER = new BigDecimal("2.00");
    public static final BigDecimal MAX_PLAUSIBLE_KM_PER_LITER = new BigDecimal("35.00");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private PrimeFuelStation station;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by_user_id", nullable = false)
    private User registeredBy;

    @Column(name = "fueled_on", nullable = false)
    private LocalDate fueledOn;

    @Column(name = "odometer_km", nullable = false)
    private long odometerKm;

    @Column(name = "previous_odometer_km")
    private Long previousOdometerKm;

    @Column(name = "distance_driven_km")
    private Long distanceDrivenKm;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal liters;

    @Column(name = "total_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Column(name = "consumption_km_per_liter", precision = 10, scale = 2)
    private BigDecimal consumptionKmPerLiter;

    @Column(name = "reference_average_km_per_liter", precision = 10, scale = 2)
    private BigDecimal referenceAverageKmPerLiter;

    @Column(name = "out_of_pattern", nullable = false)
    private boolean outOfPattern = false;

    @Column(name = "anomaly_reason", length = 255)
    private String anomalyReason;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Fueling() {
    }

    private Fueling(
            Vehicle vehicle,
            PrimeFuelStation station,
            User registeredBy,
            LocalDate fueledOn,
            long odometerKm,
            Long previousOdometerKm,
            Long distanceDrivenKm,
            BigDecimal liters,
            BigDecimal totalValue,
            FuelType fuelType,
            BigDecimal consumptionKmPerLiter,
            BigDecimal referenceAverageKmPerLiter,
            boolean outOfPattern,
            String anomalyReason
    ) {
        this.vehicle = Objects.requireNonNull(vehicle, "Veículo é obrigatório");
        this.station = Objects.requireNonNull(station, "Posto credenciado Prime é obrigatório");
        this.registeredBy = Objects.requireNonNull(registeredBy, "Usuário responsável é obrigatório");
        this.fueledOn = Objects.requireNonNull(fueledOn, "Data do abastecimento é obrigatória");
        this.odometerKm = odometerKm;
        this.previousOdometerKm = previousOdometerKm;
        this.distanceDrivenKm = distanceDrivenKm;
        this.liters = liters;
        this.totalValue = totalValue;
        this.fuelType = Objects.requireNonNull(fuelType, "Tipo de combustível é obrigatório");
        this.consumptionKmPerLiter = consumptionKmPerLiter;
        this.referenceAverageKmPerLiter = referenceAverageKmPerLiter;
        this.outOfPattern = outOfPattern;
        this.anomalyReason = anomalyReason;
    }

    public static Fueling register(
            Vehicle vehicle,
            PrimeFuelStation station,
            User registeredBy,
            LocalDate fueledOn,
            long odometerKm,
            Long previousOdometerKm,
            BigDecimal liters,
            BigDecimal totalValue,
            FuelType fuelType,
            BigDecimal referenceAverageKmPerLiter
    ) {
        if (odometerKm < 0) {
            throw new InvalidRequestException("A quilometragem do abastecimento não pode ser negativa");
        }
        if (liters == null || liters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("A quantidade de litros deve ser maior que zero");
        }
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("O valor do abastecimento deve ser maior que zero");
        }

        BigDecimal normalizedLiters = liters.setScale(3, RoundingMode.HALF_UP);
        BigDecimal normalizedValue = totalValue.setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedReference = referenceAverageKmPerLiter == null
                ? null
                : referenceAverageKmPerLiter.setScale(2, RoundingMode.HALF_UP);

        Long distanceKm = null;
        BigDecimal consumption = null;
        boolean anomaly = false;
        String reason = null;

        if (previousOdometerKm != null) {
            if (odometerKm < previousOdometerKm) {
                throw new InvalidRequestException("A quilometragem do abastecimento não pode ser menor que a anterior");
            }
            long delta = odometerKm - previousOdometerKm;
            if (delta > 0) {
                distanceKm = delta;
                consumption = BigDecimal.valueOf(delta)
                        .divide(normalizedLiters, 2, RoundingMode.HALF_UP);
                reason = evaluateAnomalyReason(consumption, normalizedReference);
                anomaly = reason != null;
            }
        }

        return new Fueling(
                vehicle,
                station,
                registeredBy,
                fueledOn,
                odometerKm,
                distanceKm == null ? null : previousOdometerKm,
                distanceKm,
                normalizedLiters,
                normalizedValue,
                fuelType,
                consumption,
                normalizedReference,
                anomaly,
                reason
        );
    }

    private static String evaluateAnomalyReason(BigDecimal consumption, BigDecimal referenceAverage) {
        if (referenceAverage != null && referenceAverage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal minExpected = referenceAverage.multiply(LOW_EFFICIENCY_MULTIPLIER);
            if (consumption.compareTo(minExpected) <= 0) {
                BigDecimal dropPct = referenceAverage.subtract(consumption)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(referenceAverage, 1, RoundingMode.HALF_UP);
                return String.format(
                        Locale.ROOT,
                        "Consumo de %s km/L ficou %s%% abaixo da média histórica do veículo (%s km/L)",
                        consumption.toPlainString(),
                        dropPct.toPlainString(),
                        referenceAverage.toPlainString()
                );
            }
            BigDecimal maxExpected = referenceAverage.multiply(HIGH_EFFICIENCY_MULTIPLIER);
            if (consumption.compareTo(maxExpected) >= 0) {
                BigDecimal spikePct = consumption.subtract(referenceAverage)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(referenceAverage, 1, RoundingMode.HALF_UP);
                return String.format(
                        Locale.ROOT,
                        "Consumo de %s km/L ficou %s%% acima da média histórica do veículo (%s km/L)",
                        consumption.toPlainString(),
                        spikePct.toPlainString(),
                        referenceAverage.toPlainString()
                );
            }
        }

        if (consumption.compareTo(MIN_PLAUSIBLE_KM_PER_LITER) < 0) {
            return String.format(
                    Locale.ROOT,
                    "Consumo de %s km/L abaixo do limite operacional mínimo (%s km/L)",
                    consumption.toPlainString(),
                    MIN_PLAUSIBLE_KM_PER_LITER.toPlainString()
            );
        }
        if (consumption.compareTo(MAX_PLAUSIBLE_KM_PER_LITER) > 0) {
            return String.format(
                    Locale.ROOT,
                    "Consumo de %s km/L acima do limite operacional máximo (%s km/L)",
                    consumption.toPlainString(),
                    MAX_PLAUSIBLE_KM_PER_LITER.toPlainString()
            );
        }
        return null;
    }

    public UUID getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public PrimeFuelStation getStation() {
        return station;
    }

    public User getRegisteredBy() {
        return registeredBy;
    }

    public LocalDate getFueledOn() {
        return fueledOn;
    }

    public long getOdometerKm() {
        return odometerKm;
    }

    public Long getPreviousOdometerKm() {
        return previousOdometerKm;
    }

    public Long getDistanceDrivenKm() {
        return distanceDrivenKm;
    }

    public BigDecimal getLiters() {
        return liters;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public BigDecimal getConsumptionKmPerLiter() {
        return consumptionKmPerLiter;
    }

    public BigDecimal getReferenceAverageKmPerLiter() {
        return referenceAverageKmPerLiter;
    }

    public boolean isOutOfPattern() {
        return outOfPattern;
    }

    public String getAnomalyReason() {
        return anomalyReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

