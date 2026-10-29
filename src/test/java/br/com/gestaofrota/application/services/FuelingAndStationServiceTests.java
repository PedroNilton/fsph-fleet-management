package br.com.gestaofrota.application.services;

import br.com.gestaofrota.api.v1.dtos.responses.FleetConsumptionReportResponse;
import br.com.gestaofrota.api.v1.dtos.responses.VehicleConsumptionSummaryResponse;
import br.com.gestaofrota.domain.enums.FuelType;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.models.Fueling;
import br.com.gestaofrota.domain.models.PrimeFuelStation;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.FuelingRepository;
import br.com.gestaofrota.infra.repositories.PrimeFuelStationRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelingAndStationServiceTests {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 10, 28);

    @Mock
    private PrimeFuelStationRepository stationRepository;

    @Mock
    private FuelingRepository fuelingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    private PrimeFuelStationService stationService;
    private FuelingService fuelingService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);
        stationService = new PrimeFuelStationService(stationRepository);
        fuelingService = new FuelingService(fuelingRepository, stationRepository, vehicleRepository, userRepository, clock);
    }

    @Test
    void createsPrimeStationNormalizingCnpjAndStateAndRejectsDuplicate() {
        when(stationRepository.existsByCnpj("12345678000190")).thenReturn(false);
        when(stationRepository.saveAndFlush(any(PrimeFuelStation.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        PrimeFuelStation created = stationService.create(
                " Posto Prime Jardins ",
                "12.345.678/0001-90",
                " Aracaju ",
                " se ",
                " Av. Ministro Geraldo Barreto Sobral, 215 "
        );

        assertThat(created.getName()).isEqualTo("Posto Prime Jardins");
        assertThat(created.getCnpj()).isEqualTo("12345678000190");
        assertThat(created.getCity()).isEqualTo("Aracaju");
        assertThat(created.getState()).isEqualTo("SE");
        assertThat(created.isActive()).isTrue();

        when(stationRepository.existsByCnpj("12345678000190")).thenReturn(true);
        assertThatThrownBy(() -> stationService.create(
                "Outro Posto",
                "12345678000190",
                "Aracaju",
                "SE",
                "Rua A, 10"
        )).isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> stationService.create(
                "Posto Inválido",
                "00.000.000/0000-00",
                "Aracaju",
                "SE",
                "Rua B, 20"
        )).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsFuelingAtInactivePrimeStation() {
        User motorist = userWithId("Motorista João", UserRole.MOTORISTA);
        PrimeFuelStation station = stationWithId("Posto Prime Norte", "11222333000181");
        station.deactivate();
        Vehicle vehicle = vehicleWithId("ABC1234", 10_000L);

        when(userRepository.findById(motorist.getId())).thenReturn(Optional.of(motorist));
        when(stationRepository.findById(station.getId())).thenReturn(Optional.of(station));

        assertThatThrownBy(() -> fuelingService.register(
                motorist.getId(),
                vehicle.getId(),
                station.getId(),
                FIXED_TODAY,
                10_400L,
                new BigDecimal("40.000"),
                new BigDecimal("240.00"),
                FuelType.GASOLINE
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Posto credenciado Prime inativo");
    }

    @Test
    void registersFuelingAdvancesOdometerAndRejectsNonIncreasingOdometer() {
        User motorist = userWithId("Motorista Ana", UserRole.MOTORISTA);
        PrimeFuelStation station = stationWithId("Posto Prime Centro", "22333444000155");
        Vehicle vehicle = vehicleWithId("DEF5678", 10_000L);

        when(userRepository.findById(motorist.getId())).thenReturn(Optional.of(motorist));
        when(stationRepository.findById(station.getId())).thenReturn(Optional.of(station));
        when(vehicleRepository.lockById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(fuelingRepository.findByVehicleIdOrderByOdometerKmDescCreatedAtDesc(vehicle.getId()))
                .thenReturn(List.of());
        when(fuelingRepository.saveAndFlush(any(Fueling.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        Fueling first = fuelingService.register(
                motorist.getId(),
                vehicle.getId(),
                station.getId(),
                FIXED_TODAY.minusDays(5),
                10_400L,
                new BigDecimal("40.000"),
                new BigDecimal("236.00"),
                FuelType.GASOLINE
        );

        assertThat(first.getDistanceDrivenKm()).isEqualTo(400L);
        assertThat(first.getConsumptionKmPerLiter()).isEqualByComparingTo("10.00");
        assertThat(first.isOutOfPattern()).isFalse();
        assertThat(vehicle.getCurrentOdometerKm()).isEqualTo(10_400L);

        when(fuelingRepository.findByVehicleIdOrderByOdometerKmDescCreatedAtDesc(vehicle.getId()))
                .thenReturn(List.of(first));

        assertThatThrownBy(() -> fuelingService.register(
                motorist.getId(),
                vehicle.getId(),
                station.getId(),
                FIXED_TODAY,
                10_400L,
                new BigDecimal("20.000"),
                new BigDecimal("120.00"),
                FuelType.GASOLINE
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void flagsOutOfPatternAlertWhenConsumptionDropsSignificantlyBelowHistoricalAverage() {
        User motorist = userWithId("Motorista Carlos", UserRole.MOTORISTA);
        PrimeFuelStation station = stationWithId("Posto Prime Sul", "33444555000122");
        Vehicle vehicle = vehicleWithId("GHI9012", 10_400L);

        Fueling baselineMeasured = withId(Fueling.register(
                vehicle,
                station,
                motorist,
                FIXED_TODAY.minusDays(10),
                10_400L,
                10_000L,
                new BigDecimal("40.000"),
                new BigDecimal("240.00"),
                FuelType.GASOLINE,
                null
        ), UUID.randomUUID());

        when(userRepository.findById(motorist.getId())).thenReturn(Optional.of(motorist));
        when(stationRepository.findById(station.getId())).thenReturn(Optional.of(station));
        when(vehicleRepository.lockById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(fuelingRepository.findByVehicleIdOrderByOdometerKmDescCreatedAtDesc(vehicle.getId()))
                .thenReturn(List.of(baselineMeasured));
        when(fuelingRepository.saveAndFlush(any(Fueling.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        Fueling anomalous = fuelingService.register(
                motorist.getId(),
                vehicle.getId(),
                station.getId(),
                FIXED_TODAY,
                10_725L,
                new BigDecimal("50.000"),
                new BigDecimal("300.00"),
                FuelType.GASOLINE
        );

        assertThat(anomalous.getConsumptionKmPerLiter()).isEqualByComparingTo("6.50");
        assertThat(anomalous.getReferenceAverageKmPerLiter()).isEqualByComparingTo("10.00");
        assertThat(anomalous.isOutOfPattern()).isTrue();
        assertThat(anomalous.getAnomalyReason()).contains("35.0% abaixo da média histórica");
    }

    @Test
    void comparesFleetConsumptionCalculatingWeightedAveragesAndDeviationPercent() {
        User motorist = userWithId("Operador Pedro", UserRole.GESTOR_FROTA);
        PrimeFuelStation station = stationWithId("Posto Prime Matriz", "44555666000177");
        Vehicle economical = vehicleWithId("AAA1111", 10_600L);
        Vehicle heavy = vehicleWithId("BBB2222", 20_300L);

        Fueling economicalFueling = withId(Fueling.register(
                economical,
                station,
                motorist,
                FIXED_TODAY.minusDays(2),
                10_600L,
                10_000L,
                new BigDecimal("50.000"),
                new BigDecimal("300.00"),
                FuelType.GASOLINE,
                null
        ), UUID.randomUUID());

        Fueling heavyFueling = withId(Fueling.register(
                heavy,
                station,
                motorist,
                FIXED_TODAY.minusDays(1),
                20_300L,
                20_000L,
                new BigDecimal("50.000"),
                new BigDecimal("310.00"),
                FuelType.DIESEL_S10,
                null
        ), UUID.randomUUID());

        when(vehicleRepository.findAll(Sort.by("plate"))).thenReturn(List.of(economical, heavy));
        when(fuelingRepository.findAllByOrderByFueledOnDescCreatedAtDesc())
                .thenReturn(List.of(heavyFueling, economicalFueling));

        FleetConsumptionReportResponse report = fuelingService.compareFleetConsumption();

        assertThat(report.totalFuelings()).isEqualTo(2);
        assertThat(report.totalFleetLiters()).isEqualByComparingTo("100.000");
        assertThat(report.totalFleetSpent()).isEqualByComparingTo("610.00");
        assertThat(report.fleetAverageKmPerLiter()).isEqualByComparingTo("9.00");
        assertThat(report.vehicles()).hasSize(2);

        VehicleConsumptionSummaryResponse first = report.vehicles().get(0);
        assertThat(first.vehiclePlate()).isEqualTo("AAA1111");
        assertThat(first.averageKmPerLiter()).isEqualByComparingTo("12.00");
        assertThat(first.deviationFromFleetPercent()).isEqualByComparingTo("33.3");

        VehicleConsumptionSummaryResponse second = report.vehicles().get(1);
        assertThat(second.vehiclePlate()).isEqualTo("BBB2222");
        assertThat(second.averageKmPerLiter()).isEqualByComparingTo("6.00");
        assertThat(second.deviationFromFleetPercent()).isEqualByComparingTo("-33.3");
    }

    private static User userWithId(String name, UserRole role) {
        User user = new User(new Sector("Transporte"), name, UUID.randomUUID() + "@exemplo.com", "hash", role);
        return withId(user, UUID.randomUUID());
    }

    private static PrimeFuelStation stationWithId(String name, String cnpj) {
        PrimeFuelStation station = new PrimeFuelStation(name, cnpj, "Aracaju", "SE", "Av. Beira Mar, 100");
        return withId(station, UUID.randomUUID());
    }

    private static Vehicle vehicleWithId(String plate, long odometerKm) {
        Vehicle vehicle = new Vehicle(new Sector("Frota"), plate, "Oroch", 5, "Operacional", odometerKm);
        return withId(vehicle, UUID.randomUUID());
    }

    private static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}

