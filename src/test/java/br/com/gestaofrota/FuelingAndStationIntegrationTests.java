package br.com.gestaofrota;

import br.com.gestaofrota.api.v1.dtos.responses.FleetConsumptionReportResponse;
import br.com.gestaofrota.application.services.FuelingService;
import br.com.gestaofrota.application.services.PrimeFuelStationService;
import br.com.gestaofrota.application.services.VehicleService;
import br.com.gestaofrota.domain.enums.FuelType;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.models.Fueling;
import br.com.gestaofrota.domain.models.PrimeFuelStation;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FuelingAndStationIntegrationTests extends PostgresIntegrationTest {

    @Autowired
    private SectorRepository sectors;

    @Autowired
    private UserRepository users;

    @Autowired
    private VehicleService vehicles;

    @Autowired
    private PrimeFuelStationService stations;

    @Autowired
    private FuelingService fuelings;

    @Test
    void managesPrimeStationsTracksVehicleConsumptionAndTriggersOutOfPatternAlert() {
        Sector sector = sectors.save(new Sector("Operações " + UUID.randomUUID()));
        User motorist = users.save(new User(
                sector,
                "Motorista Operacional",
                "motorista-" + UUID.randomUUID() + "@fsph.se.gov.br",
                "hash",
                UserRole.MOTORISTA
        ));
        Vehicle vehicle = vehicles.create(sector.getId(), randomPlate(), "Duster", 5, "Fiscalização", 20_000L);

        String cnpj = randomCnpj();
        PrimeFuelStation station = stations.create(
                "Posto Prime Atalaia",
                cnpj,
                "Aracaju",
                "SE",
                "Av. Santos Dumont, 500"
        );

        assertThatThrownBy(() -> stations.create("Duplicado", cnpj, "Aracaju", "SE", "Rua B, 1"))
                .isInstanceOf(ConflictException.class);

        stations.deactivate(station.getId());
        assertThatThrownBy(() -> fuelings.register(
                motorist.getId(),
                vehicle.getId(),
                station.getId(),
                LocalDate.now(),
                20_400L,
                new BigDecimal("40.000"),
                new BigDecimal("240.00"),
                FuelType.GASOLINE
        )).isInstanceOf(ConflictException.class);

        stations.reactivate(station.getId());

        Fueling normalFueling = fuelings.register(
                motorist.getId(),
                vehicle.getId(),
                station.getId(),
                LocalDate.now().minusDays(3),
                20_400L,
                new BigDecimal("40.000"),
                new BigDecimal("240.00"),
                FuelType.GASOLINE
        );
        assertThat(normalFueling.getConsumptionKmPerLiter()).isEqualByComparingTo("10.00");
        assertThat(normalFueling.isOutOfPattern()).isFalse();

        Fueling anomalyFueling = fuelings.register(
                motorist.getId(),
                vehicle.getId(),
                station.getId(),
                LocalDate.now(),
                20_700L,
                new BigDecimal("50.000"),
                new BigDecimal("305.00"),
                FuelType.GASOLINE
        );
        assertThat(anomalyFueling.getConsumptionKmPerLiter()).isEqualByComparingTo("6.00");
        assertThat(anomalyFueling.isOutOfPattern()).isTrue();
        assertThat(vehicles.find(vehicle.getId()).getCurrentOdometerKm()).isEqualTo(20_700L);

        List<Fueling> alerts = fuelings.listAnomalyAlerts(vehicle.getId());
        assertThat(alerts).extracting(Fueling::getId).containsExactly(anomalyFueling.getId());

        FleetConsumptionReportResponse fleetReport = fuelings.compareFleetConsumption();
        assertThat(fleetReport.vehicles())
                .filteredOn(summary -> summary.vehicleId().equals(vehicle.getId()))
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.totalFuelings()).isEqualTo(2);
                    assertThat(summary.totalMeasuredDistanceKm()).isEqualTo(700L);
                    assertThat(summary.totalLiters()).isEqualByComparingTo("90.000");
                    assertThat(summary.averageKmPerLiter()).isEqualByComparingTo("7.78");
                    assertThat(summary.anomalyCount()).isEqualTo(1);
                });
    }

    private static String randomPlate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }

    private static String randomCnpj() {
        long number = ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L);
        return Long.toString(number);
    }
}

