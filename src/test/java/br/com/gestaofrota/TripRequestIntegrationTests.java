package br.com.gestaofrota;

import br.com.gestaofrota.api.v1.dtos.responses.FleetCalendarResponse;
import br.com.gestaofrota.api.v1.dtos.responses.TripRequestAvailabilityResponse;
import br.com.gestaofrota.application.services.DriverLicenseService;
import br.com.gestaofrota.application.services.DriverService;
import br.com.gestaofrota.application.services.MaintenanceService;
import br.com.gestaofrota.application.services.NotificationService;
import br.com.gestaofrota.application.services.TripRequestService;
import br.com.gestaofrota.application.services.VehicleService;
import br.com.gestaofrota.domain.enums.MaintenanceStatus;
import br.com.gestaofrota.domain.enums.MaintenanceType;
import br.com.gestaofrota.domain.enums.NotificationType;
import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.UserNotification;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TripRequestIntegrationTests extends PostgresIntegrationTest {

    @Autowired
    private SectorRepository sectors;

    @Autowired
    private UserRepository users;

    @Autowired
    private VehicleService vehicles;

    @Autowired
    private DriverService drivers;

    @Autowired
    private DriverLicenseService driverLicenses;

    @Autowired
    private MaintenanceService maintenances;

    @Autowired
    private TripRequestService tripRequests;

    @Autowired
    private NotificationService notifications;

    @Test
    void executesFullLifecycleWithCapacityOverlapMaintenanceCalendarAndNotifications() {
        Sector sector = sectors.save(new Sector("Vigilância Sanitária " + UUID.randomUUID()));
        User employee = users.save(new User(
                sector,
                "Servidor Solicitante",
                "servidor-" + UUID.randomUUID() + "@fsph.se.gov.br",
                "hash",
                UserRole.EMPLOYEE
        ));
        User manager = users.save(new User(
                sector,
                "Gestor Frota",
                "gestor-" + UUID.randomUUID() + "@fsph.se.gov.br",
                "hash",
                UserRole.GESTOR_FROTA
        ));
        User driverUser = users.save(new User(
                sector,
                "Condutor Institucional",
                "condutor-" + UUID.randomUUID() + "@fsph.se.gov.br",
                "hash",
                UserRole.MOTORISTA
        ));

        Driver driver = drivers.create(driverUser.getId());
        driverLicenses.register(
                driver.getId(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 11),
                "D",
                LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(3)
        );

        Vehicle smallCar = vehicles.create(sector.getId(), randomPlate(), "Onix", 5, "Administrativo", 10_000L);
        Vehicle van = vehicles.create(sector.getId(), randomPlate(), "Spin 7 Lugares", 7, "Institucional", 22_000L);
        Vehicle workshopVehicle = vehicles.create(sector.getId(), randomPlate(), "Master", 12, "Operacional", 40_000L);

        maintenances.register(
                workshopVehicle.getId(),
                MaintenanceType.CORRECTIVE,
                MaintenanceStatus.IN_PROGRESS,
                LocalDate.now(),
                null,
                40_100L,
                null,
                "Revisão de freios",
                "Oficina Credenciada",
                new BigDecimal("850.00"),
                null
        );

        Instant departure = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);
        Instant expectedReturn = departure.plus(4, ChronoUnit.HOURS);

        List<Vehicle> eligibleForFive = tripRequests.listEligibleVehicles(5, departure, expectedReturn);
        assertThat(eligibleForFive)
                .extracting(Vehicle::getId)
                .contains(van.getId())
                .doesNotContain(smallCar.getId(), workshopVehicle.getId());

        assertThatThrownBy(() -> tripRequests.create(
                employee.getId(),
                departure,
                expectedReturn,
                "Aracaju",
                "Itabaiana",
                "Coleta externa",
                5,
                smallCar.getId()
        )).isInstanceOf(InvalidRequestException.class);

        TripRequest created = tripRequests.create(
                employee.getId(),
                departure,
                expectedReturn,
                "Aracaju",
                "Itabaiana",
                "Coleta externa",
                5,
                van.getId()
        );
        assertThat(created.getStatus()).isEqualTo(TripRequestStatus.PENDING);

        TripRequest updated = tripRequests.updatePending(
                created.getId(),
                employee.getId(),
                departure,
                expectedReturn,
                "Sede FSPH - Aracaju",
                "Hospital Regional de Itabaiana",
                "Transporte de insumos laboratoriais",
                5,
                van.getId()
        );
        assertThat(updated.getDestination()).isEqualTo("Hospital Regional de Itabaiana");

        TripRequest toCancel = tripRequests.create(
                employee.getId(),
                departure.plus(1, ChronoUnit.DAYS),
                expectedReturn.plus(1, ChronoUnit.DAYS),
                "Aracaju",
                "Estância",
                "Reunião cancelada",
                2,
                null
        );
        assertThat(tripRequests.cancelByRequester(toCancel.getId(), employee.getId()).getStatus())
                .isEqualTo(TripRequestStatus.CANCELED);

        TripRequestAvailabilityResponse availability = tripRequests.analyzeAvailability(created.getId());
        assertThat(availability.requestedVehicleAvailable()).isTrue();
        assertThat(availability.availableVehicles()).extracting(v -> v.id()).contains(van.getId());
        assertThat(availability.availableDrivers()).extracting(d -> d.driverId()).contains(driver.getId());

        TripRequest approved = tripRequests.approve(created.getId(), manager.getId(), null, "Deferido para atendimento");
        assertThat(approved.getStatus()).isEqualTo(TripRequestStatus.APPROVED);

        TripRequest conflictingRequest = tripRequests.create(
                employee.getId(),
                departure.plus(1, ChronoUnit.HOURS),
                expectedReturn.plus(1, ChronoUnit.HOURS),
                "Aracaju",
                "Lagarto",
                "Viagem concorrente",
                5,
                null
        );
        assertThatThrownBy(() -> tripRequests.approve(
                conflictingRequest.getId(),
                manager.getId(),
                van.getId(),
                null
        )).isInstanceOf(ConflictException.class);

        TripRequest rejected = tripRequests.reject(
                conflictingRequest.getId(),
                manager.getId(),
                "Veículo de 7 lugares já reservado no mesmo horário"
        );
        assertThat(rejected.getStatus()).isEqualTo(TripRequestStatus.REJECTED);

        TripRequest scheduled = tripRequests.schedule(
                created.getId(),
                manager.getId(),
                van.getId(),
                driver.getId()
        );
        assertThat(scheduled.getStatus()).isEqualTo(TripRequestStatus.SCHEDULED);

        assertThatThrownBy(() -> tripRequests.cancelByRequester(created.getId(), employee.getId()))
                .isInstanceOf(ConflictException.class);

        FleetCalendarResponse calendar = tripRequests.getFleetCalendar(
                departure.minus(1, ChronoUnit.HOURS),
                expectedReturn.plus(1, ChronoUnit.HOURS)
        );
        assertThat(calendar.reservations())
                .extracting(r -> r.tripRequestId())
                .contains(scheduled.getId());

        List<UserNotification> userNotifications = notifications.listForRecipient(employee.getId(), false);
        assertThat(userNotifications)
                .extracting(UserNotification::getType)
                .containsExactlyInAnyOrder(
                        NotificationType.TRIP_REQUEST_APPROVED,
                        NotificationType.TRIP_REQUEST_REJECTED,
                        NotificationType.TRIP_REQUEST_SCHEDULED
                );
        assertThat(notifications.countUnread(employee.getId())).isEqualTo(3L);
        assertThat(notifications.markAllAsRead(employee.getId())).isEqualTo(3);
        assertThat(notifications.countUnread(employee.getId())).isZero();
    }

    private static String randomPlate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
    }
}
