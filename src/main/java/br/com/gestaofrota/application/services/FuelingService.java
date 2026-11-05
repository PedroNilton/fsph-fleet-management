package br.com.gestaofrota.application.services;

import br.com.gestaofrota.api.v1.dtos.responses.FleetConsumptionReportResponse;
import br.com.gestaofrota.api.v1.dtos.responses.VehicleConsumptionSummaryResponse;
import br.com.gestaofrota.domain.enums.FuelType;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.models.Fueling;
import br.com.gestaofrota.domain.models.PrimeFuelStation;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.FuelingRepository;
import br.com.gestaofrota.infra.repositories.PrimeFuelStationRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FuelingService {

    private final FuelingRepository fuelings;
    private final PrimeFuelStationRepository stations;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final Clock clock;

    @Autowired
    public FuelingService(
            FuelingRepository fuelings,
            PrimeFuelStationRepository stations,
            VehicleRepository vehicles,
            UserRepository users
    ) {
        this(fuelings, stations, vehicles, users, Clock.systemDefaultZone());
    }

    FuelingService(
            FuelingRepository fuelings,
            PrimeFuelStationRepository stations,
            VehicleRepository vehicles,
            UserRepository users,
            Clock clock
    ) {
        this.fuelings = fuelings;
        this.stations = stations;
        this.vehicles = vehicles;
        this.users = users;
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public Fueling register(
            UUID authenticatedUserId,
            UUID vehicleId,
            UUID stationId,
            LocalDate fueledOn,
            long odometerKm,
            BigDecimal liters,
            BigDecimal totalValue,
            FuelType fuelType
    ) {
        if (fueledOn == null) {
            throw new InvalidRequestException("Data do abastecimento é obrigatória");
        }
        if (fueledOn.isAfter(LocalDate.now(clock))) {
            throw new InvalidRequestException("A data do abastecimento não pode ser futura");
        }

        User registeredBy = users.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", authenticatedUserId));
        if (!registeredBy.isActive()) {
            throw new ConflictException("Usuário inativo não pode registrar abastecimento");
        }

        PrimeFuelStation station = stations.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Posto credenciado Prime", stationId));
        if (!station.isActive()) {
            throw new ConflictException("Posto credenciado Prime inativo não pode receber registros de abastecimento");
        }

        Vehicle vehicle = vehicles.lockById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo", vehicleId));
        if (!vehicle.isActive()) {
            throw new ConflictException("Veículo inativo não pode receber abastecimento");
        }
        if (odometerKm < vehicle.getCurrentOdometerKm()) {
            throw new ConflictException("A quilometragem do abastecimento não pode ser menor que a atual do veículo");
        }

        List<Fueling> existingFuelings = fuelings.findByVehicleIdOrderByOdometerKmDescCreatedAtDesc(vehicleId);
        BigDecimal historicalAverage = computeMeasuredAverageKmPerLiter(existingFuelings);

        Long previousOdometerKm;
        if (!existingFuelings.isEmpty()) {
            Fueling latest = existingFuelings.get(0);
            if (odometerKm <= latest.getOdometerKm()) {
                throw new ConflictException("A quilometragem do abastecimento deve ser maior que a do último abastecimento registrado");
            }
            previousOdometerKm = latest.getOdometerKm();
        } else {
            previousOdometerKm = odometerKm > vehicle.getCurrentOdometerKm()
                    ? vehicle.getCurrentOdometerKm()
                    : null;
        }

        Fueling created = Fueling.register(
                vehicle,
                station,
                registeredBy,
                fueledOn,
                odometerKm,
                previousOdometerKm,
                liters,
                totalValue,
                fuelType,
                historicalAverage
        );

        if (odometerKm > vehicle.getCurrentOdometerKm()) {
            vehicle.updateCurrentOdometerKm(odometerKm);
        }

        return fuelings.saveAndFlush(created);
    }

    @Transactional(readOnly = true)
    public List<Fueling> list(UUID vehicleId, UUID stationId, FuelType fuelType, Boolean outOfPatternOnly) {
        List<Fueling> base = vehicleId != null
                ? listByVehicle(vehicleId)
                : fuelings.findAllByOrderByFueledOnDescCreatedAtDesc();

        return base.stream()
                .filter(item -> stationId == null || item.getStation().getId().equals(stationId))
                .filter(item -> fuelType == null || item.getFuelType() == fuelType)
                .filter(item -> !Boolean.TRUE.equals(outOfPatternOnly) || item.isOutOfPattern())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Fueling> listMine(UUID authenticatedUserId) {
        return fuelings.findByRegisteredByIdOrderByFueledOnDescCreatedAtDesc(authenticatedUserId);
    }

    @Transactional(readOnly = true)
    public List<Fueling> listByVehicle(UUID vehicleId) {
        findVehicle(vehicleId);
        return fuelings.findByVehicleIdOrderByOdometerKmDescCreatedAtDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public Fueling find(UUID id) {
        return fuelings.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abastecimento", id));
    }

    @Transactional(readOnly = true)
    public List<Fueling> listAnomalyAlerts(UUID vehicleId) {
        if (vehicleId != null) {
            findVehicle(vehicleId);
        }
        return fuelings.findByOutOfPatternTrueOrderByFueledOnDescCreatedAtDesc().stream()
                .filter(item -> vehicleId == null || item.getVehicle().getId().equals(vehicleId))
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleConsumptionSummaryResponse summarizeVehicleConsumption(UUID vehicleId) {
        Vehicle vehicle = findVehicle(vehicleId);
        List<Fueling> allFuelings = fuelings.findAllByOrderByFueledOnDescCreatedAtDesc();
        BigDecimal fleetAverage = computeMeasuredAverageKmPerLiter(allFuelings);

        List<Fueling> vehicleFuelings = allFuelings.stream()
                .filter(item -> item.getVehicle().getId().equals(vehicleId))
                .toList();
        return buildVehicleSummary(vehicle, vehicleFuelings, fleetAverage);
    }

    @Transactional(readOnly = true)
    public FleetConsumptionReportResponse compareFleetConsumption() {
        List<Vehicle> allVehicles = vehicles.findAll(Sort.by("plate"));
        List<Fueling> allFuelings = fuelings.findAllByOrderByFueledOnDescCreatedAtDesc();

        BigDecimal fleetAverage = computeMeasuredAverageKmPerLiter(allFuelings);
        BigDecimal totalFleetLiters = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        BigDecimal totalFleetSpent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int totalAnomalyAlerts = 0;

        for (Fueling item : allFuelings) {
            totalFleetLiters = totalFleetLiters.add(item.getLiters());
            totalFleetSpent = totalFleetSpent.add(item.getTotalValue());
            if (item.isOutOfPattern()) {
                totalAnomalyAlerts++;
            }
        }

        Map<UUID, List<Fueling>> byVehicle = allFuelings.stream()
                .collect(Collectors.groupingBy(item -> item.getVehicle().getId()));

        List<VehicleConsumptionSummaryResponse> summaries = new ArrayList<>();
        for (Vehicle vehicle : allVehicles) {
            List<Fueling> vehicleFuelings = byVehicle.getOrDefault(vehicle.getId(), List.of());
            if (vehicle.isActive() || !vehicleFuelings.isEmpty()) {
                summaries.add(buildVehicleSummary(vehicle, vehicleFuelings, fleetAverage));
            }
        }

        return new FleetConsumptionReportResponse(
                fleetAverage,
                totalFleetLiters,
                totalFleetSpent,
                allFuelings.size(),
                totalAnomalyAlerts,
                summaries
        );
    }

    private VehicleConsumptionSummaryResponse buildVehicleSummary(
            Vehicle vehicle,
            List<Fueling> vehicleFuelings,
            BigDecimal fleetAverage
    ) {
        BigDecimal totalLiters = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        BigDecimal totalSpent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long totalMeasuredDistanceKm = 0L;
        int anomalyCount = 0;
        LocalDate lastFueledOn = null;

        for (Fueling item : vehicleFuelings) {
            totalLiters = totalLiters.add(item.getLiters());
            totalSpent = totalSpent.add(item.getTotalValue());
            if (item.getDistanceDrivenKm() != null && item.getDistanceDrivenKm() > 0) {
                totalMeasuredDistanceKm += item.getDistanceDrivenKm();
            }
            if (item.isOutOfPattern()) {
                anomalyCount++;
            }
            if (lastFueledOn == null || item.getFueledOn().isAfter(lastFueledOn)) {
                lastFueledOn = item.getFueledOn();
            }
        }

        BigDecimal averageKmPerLiter = computeMeasuredAverageKmPerLiter(vehicleFuelings);
        BigDecimal deviationPercent = null;
        if (averageKmPerLiter != null
                && fleetAverage != null
                && fleetAverage.compareTo(BigDecimal.ZERO) > 0) {
            deviationPercent = averageKmPerLiter.subtract(fleetAverage)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(fleetAverage, 1, RoundingMode.HALF_UP);
        }

        return new VehicleConsumptionSummaryResponse(
                vehicle.getId(),
                vehicle.getPlate(),
                vehicle.getModel(),
                vehicle.getUsageType(),
                vehicle.getCurrentOdometerKm(),
                vehicleFuelings.size(),
                totalLiters,
                totalSpent,
                totalMeasuredDistanceKm,
                averageKmPerLiter,
                fleetAverage,
                deviationPercent,
                anomalyCount,
                lastFueledOn
        );
    }

    private BigDecimal computeMeasuredAverageKmPerLiter(List<Fueling> records) {
        long measuredDistance = 0L;
        BigDecimal measuredLiters = BigDecimal.ZERO;
        for (Fueling item : records) {
            if (item.getDistanceDrivenKm() != null && item.getDistanceDrivenKm() > 0) {
                measuredDistance += item.getDistanceDrivenKm();
                measuredLiters = measuredLiters.add(item.getLiters());
            }
        }
        if (measuredDistance <= 0L || measuredLiters.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return BigDecimal.valueOf(measuredDistance)
                .divide(measuredLiters, 2, RoundingMode.HALF_UP);
    }

    private Vehicle findVehicle(UUID id) {
        return vehicles.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo", id));
    }
}

