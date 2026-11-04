package br.com.gestaofrota.application.services;

import br.com.gestaofrota.api.v1.dtos.responses.FleetCalendarResponse;
import br.com.gestaofrota.api.v1.dtos.responses.TripRequestAvailabilityResponse;
import br.com.gestaofrota.domain.enums.NotificationType;
import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.exceptions.UnauthorizedAccessException;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.DriverLicense;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.UserNotification;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.config.notifications.NotificationEmailProperties;
import br.com.gestaofrota.infra.notifications.SmtpNotificationEmailSender;
import br.com.gestaofrota.infra.repositories.DriverLicenseRepository;
import br.com.gestaofrota.infra.repositories.DriverRepository;
import br.com.gestaofrota.infra.repositories.TripRequestRepository;
import br.com.gestaofrota.infra.repositories.UserNotificationRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripRequestAndNotificationServiceTests {

    @Mock
    private TripRequestRepository tripRequests;

    @Mock
    private UserRepository users;

    @Mock
    private VehicleRepository vehicles;

    @Mock
    private DriverRepository drivers;

    @Mock
    private DriverLicenseRepository driverLicenses;

    @Mock
    private MaintenanceAvailabilityGuard maintenanceGuard;

    @Mock
    private UserNotificationRepository notificationRepository;

    @Mock
    private NotificationEmailSender emailSender;

    private NotificationService notificationService;
    private TripRequestService service;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, emailSender);
        service = new TripRequestService(
                tripRequests,
                users,
                vehicles,
                drivers,
                driverLicenses,
                maintenanceGuard,
                notificationService
        );
        lenient().when(notificationRepository.save(any(UserNotification.class))).thenAnswer(invocation -> {
            UserNotification saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            }
            return saved;
        });
        lenient().when(tripRequests.save(any(TripRequest.class))).thenAnswer(invocation -> {
            TripRequest saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            }
            return saved;
        });
    }

    @Test
    void filtersEligibleVehiclesSubtractingDriverSeatAndExcludingOverlappingReservations() {
        Vehicle sedan5Seats = buildVehicle("AAA1111", "Cronos", 5, false);
        Vehicle van8Seats = buildVehicle("BBB2222", "Spin 7L", 8, false);
        Vehicle busyVan = buildVehicle("CCC3333", "Sprinter", 15, false);

        Instant departure = Instant.parse("2026-11-10T12:00:00Z");
        Instant expectedReturn = Instant.parse("2026-11-10T16:00:00Z");

        User existingRequester = buildUser("Servidor", UserRole.EMPLOYEE);
        TripRequest existingReservation = new TripRequest(
                existingRequester,
                departure.minus(1, ChronoUnit.HOURS),
                departure.plus(1, ChronoUnit.HOURS),
                "Sede",
                "Hospital",
                "Transporte",
                4,
                busyVan
        );
        ReflectionTestUtils.setField(existingReservation, "id", UUID.randomUUID());
        existingReservation.approve(buildUser("Gestor", UserRole.GESTOR_FROTA), busyVan, null, Instant.now());

        when(vehicles.findByActiveTrueAndInMaintenanceFalseOrderByPlateAsc())
                .thenReturn(List.of(sedan5Seats, van8Seats, busyVan));
        when(tripRequests.findActiveReservationsOverlapping(any(), eq(departure), eq(expectedReturn)))
                .thenReturn(List.of(existingReservation));

        List<Vehicle> forFivePassengers = service.listEligibleVehicles(5, departure, expectedReturn);
        assertThat(forFivePassengers)
                .extracting(Vehicle::getId)
                .containsExactly(van8Seats.getId());

        List<Vehicle> forFourPassengers = service.listEligibleVehicles(4, departure, expectedReturn);
        assertThat(forFourPassengers)
                .extracting(Vehicle::getId)
                .containsExactly(sedan5Seats.getId(), van8Seats.getId());
    }

    @Test
    void enforcesOwnershipAndPendingStateForUpdateAndCancel() {
        User owner = buildUser("Solicitante", UserRole.EMPLOYEE);
        User stranger = buildUser("Outro Servidor", UserRole.EMPLOYEE);
        Vehicle sedan = buildVehicle("DDD4444", "Onix", 5, false);

        Instant departure = Instant.parse("2026-11-12T10:00:00Z");
        Instant expectedReturn = Instant.parse("2026-11-12T14:00:00Z");

        when(users.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(vehicles.findById(sedan.getId())).thenReturn(Optional.of(sedan));
        when(tripRequests.findVehicleReservationsOverlapping(eq(sedan.getId()), any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(
                owner.getId(),
                departure,
                expectedReturn,
                "Aracaju",
                "Itabaiana",
                "Visita técnica",
                5,
                sedan.getId()
        )).isInstanceOf(InvalidRequestException.class);

        TripRequest created = service.create(
                owner.getId(),
                departure,
                expectedReturn,
                "Aracaju",
                "Itabaiana",
                "Visita técnica",
                4,
                sedan.getId()
        );
        when(tripRequests.lockById(created.getId())).thenReturn(Optional.of(created));
        when(tripRequests.findWithDetailsById(created.getId())).thenReturn(Optional.of(created));

        assertThatThrownBy(() -> service.findVisibleTo(created.getId(), stranger.getId(), false))
                .isInstanceOf(UnauthorizedAccessException.class);
        assertThat(service.findVisibleTo(created.getId(), stranger.getId(), true).getId())
                .isEqualTo(created.getId());

        assertThatThrownBy(() -> service.cancelByRequester(created.getId(), stranger.getId()))
                .isInstanceOf(UnauthorizedAccessException.class);

        TripRequest updated = service.updatePending(
                created.getId(),
                owner.getId(),
                departure.plus(1, ChronoUnit.HOURS),
                expectedReturn.plus(1, ChronoUnit.HOURS),
                "Sede FSPH",
                "Lagarto",
                "Coleta laboratorial",
                3,
                sedan.getId()
        );
        assertThat(updated.getDestination()).isEqualTo("Lagarto");
        assertThat(updated.getPassengerCount()).isEqualTo(3);

        TripRequest canceled = service.cancelByRequester(created.getId(), owner.getId());
        assertThat(canceled.getStatus()).isEqualTo(TripRequestStatus.CANCELED);
        assertThat(canceled.getCanceledAt()).isNotNull();

        assertThatThrownBy(() -> service.updatePending(
                created.getId(),
                owner.getId(),
                departure,
                expectedReturn,
                "Sede",
                "Estância",
                "Nova tentativa",
                2,
                null
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void blocksOverlappingApprovalsRequiresRejectionReasonAndSendsNotificationsOnDecisionAndSchedule() {
        User requester = buildUser("Maria Servidora", UserRole.EMPLOYEE);
        User manager = buildUser("Carlos Gestor", UserRole.GESTOR_FROTA);
        Vehicle van = buildVehicle("EEE5555", "Master", 12, false);
        Driver driver = buildDriver("João Motorista");
        DriverLicense validLicense = new DriverLicense(
                driver,
                "12345678900",
                "D",
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2028, 12, 31)
        );

        Instant departure = Instant.parse("2026-11-15T08:00:00Z");
        Instant expectedReturn = Instant.parse("2026-11-15T12:00:00Z");

        TripRequest firstRequest = new TripRequest(
                requester, departure, expectedReturn, "Aracaju", "Propriá", "Supervisão", 6, van
        );
        ReflectionTestUtils.setField(firstRequest, "id", UUID.randomUUID());

        TripRequest overlappingRequest = new TripRequest(
                requester,
                Instant.parse("2026-11-15T10:00:00Z"),
                Instant.parse("2026-11-15T14:00:00Z"),
                "Aracaju",
                "Nossa Senhora do Socorro",
                "Apoio",
                4,
                van
        );
        ReflectionTestUtils.setField(overlappingRequest, "id", UUID.randomUUID());

        when(users.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(vehicles.lockById(van.getId())).thenReturn(Optional.of(van));
        when(drivers.lockById(driver.getId())).thenReturn(Optional.of(driver));
        when(driverLicenses.findByDriverIdAndStatus(driver.getId(), RecordStatus.ACTIVE))
                .thenReturn(Optional.of(validLicense));
        when(emailSender.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        when(tripRequests.lockById(firstRequest.getId())).thenReturn(Optional.of(firstRequest));
        when(tripRequests.findVehicleReservationsOverlapping(eq(van.getId()), any(), any(), any()))
                .thenReturn(List.of());
        when(tripRequests.findDriverReservationsOverlapping(eq(driver.getId()), any(), any(), any()))
                .thenReturn(List.of());

        TripRequest approved = service.approve(firstRequest.getId(), manager.getId(), null, "Prioridade institucional");
        assertThat(approved.getStatus()).isEqualTo(TripRequestStatus.APPROVED);
        assertThat(approved.getAssignedVehicle()).isEqualTo(van);

        when(tripRequests.lockById(overlappingRequest.getId())).thenReturn(Optional.of(overlappingRequest));
        when(tripRequests.findVehicleReservationsOverlapping(eq(van.getId()), any(), any(), any()))
                .thenReturn(List.of(approved));

        assertThatThrownBy(() -> service.approve(overlappingRequest.getId(), manager.getId(), van.getId(), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sobreposta");

        assertThatThrownBy(() -> service.reject(overlappingRequest.getId(), manager.getId(), "   "))
                .isInstanceOf(InvalidRequestException.class);

        TripRequest rejected = service.reject(
                overlappingRequest.getId(),
                manager.getId(),
                "Indisponibilidade de veículo no horário solicitado"
        );
        assertThat(rejected.getStatus()).isEqualTo(TripRequestStatus.REJECTED);

        when(tripRequests.findVehicleReservationsOverlapping(eq(van.getId()), any(), eq(departure), eq(expectedReturn)))
                .thenReturn(List.of(approved));
        TripRequest scheduled = service.schedule(firstRequest.getId(), manager.getId(), van.getId(), driver.getId());
        assertThat(scheduled.getStatus()).isEqualTo(TripRequestStatus.SCHEDULED);
        assertThat(scheduled.getAssignedDriver()).isEqualTo(driver);

        ArgumentCaptor<UserNotification> notificationCaptor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepository, org.mockito.Mockito.times(3)).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(UserNotification::getType)
                .containsExactly(
                        NotificationType.TRIP_REQUEST_APPROVED,
                        NotificationType.TRIP_REQUEST_REJECTED,
                        NotificationType.TRIP_REQUEST_SCHEDULED
                );
        assertThat(notificationCaptor.getAllValues()).allMatch(UserNotification::isEmailSent);
    }

    @Test
    void analyzesAvailabilityExcludingMaintenanceExpiredLicenseAndBuildsFleetCalendar() {
        User requester = buildUser("Ana", UserRole.EMPLOYEE);
        User manager = buildUser("Gestor", UserRole.GESTOR_FROTA);
        Vehicle freeVan = buildVehicle("FFF6666", "Transit", 10, false);
        Vehicle maintenanceVan = buildVehicle("GGG7777", "Ducato", 12, true);
        Vehicle reservedCar = buildVehicle("HHH8888", "Spin", 7, false);

        Driver validDriver = buildDriver("Pedro Condutor");
        Driver expiredDriver = buildDriver("Lucas CNH Vencida");

        Instant departure = Instant.parse("2026-11-20T09:00:00Z");
        Instant expectedReturn = Instant.parse("2026-11-20T17:00:00Z");

        TripRequest targetRequest = new TripRequest(
                requester, departure, expectedReturn, "Aracaju", " Tobias Barreto", "Inspeção", 5, freeVan
        );
        ReflectionTestUtils.setField(targetRequest, "id", UUID.randomUUID());

        TripRequest reservedTrip = new TripRequest(
                requester, departure, expectedReturn, "Aracaju", "Estância", "Entrega", 4, reservedCar
        );
        ReflectionTestUtils.setField(reservedTrip, "id", UUID.randomUUID());
        reservedTrip.approve(manager, reservedCar, null, Instant.now());

        DriverLicense validCnh = new DriverLicense(
                validDriver, "9988776655", "D", LocalDate.of(2023, 5, 1), LocalDate.of(2027, 5, 1)
        );
        DriverLicense expiredCnh = new DriverLicense(
                expiredDriver, "1122334455", "B", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 11, 19)
        );

        when(tripRequests.findWithDetailsById(targetRequest.getId())).thenReturn(Optional.of(targetRequest));
        when(vehicles.findByActiveTrueAndInMaintenanceFalseOrderByPlateAsc())
                .thenReturn(List.of(freeVan, reservedCar));
        when(tripRequests.findActiveReservationsOverlapping(any(), any(), any()))
                .thenReturn(List.of(reservedTrip));
        when(driverLicenses.findByStatus(RecordStatus.ACTIVE)).thenReturn(List.of(validCnh, expiredCnh));

        TripRequestAvailabilityResponse availability = service.analyzeAvailability(targetRequest.getId());
        assertThat(availability.requestedVehicleAvailable()).isTrue();
        assertThat(availability.availableVehicles()).extracting(v -> v.id()).containsExactly(freeVan.getId());
        assertThat(availability.availableDrivers()).extracting(d -> d.driverId()).containsExactly(validDriver.getId());

        when(vehicles.findAll(any(Sort.class))).thenReturn(List.of(freeVan, maintenanceVan, reservedCar));
        FleetCalendarResponse calendar = service.getFleetCalendar(departure.minus(1, ChronoUnit.HOURS), expectedReturn);
        assertThat(calendar.totalActiveVehicles()).isEqualTo(3);
        assertThat(calendar.vehiclesInMaintenance()).isEqualTo(1);
        assertThat(calendar.vehiclesWithReservations()).isEqualTo(1);
        assertThat(calendar.vehiclesCompletelyFree()).isEqualTo(1);
        assertThat(calendar.reservations()).hasSize(1);

        doThrow(new ConflictException("Veículo em manutenção"))
                .when(maintenanceGuard).assertVehicleAvailableForAllocation(maintenanceVan.getId());
        when(vehicles.lockById(maintenanceVan.getId())).thenReturn(Optional.of(maintenanceVan));
        when(tripRequests.lockById(targetRequest.getId())).thenReturn(Optional.of(targetRequest));
        when(users.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service.approve(targetRequest.getId(), manager.getId(), maintenanceVan.getId(), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void smtpSenderSafelyNoOpsWhenDisabled() {
        SmtpNotificationEmailSender disabledSender = new SmtpNotificationEmailSender(
                new NotificationEmailProperties(false, "no-reply@fsph.se.gov.br", "", 587, "", "", true, true)
        );
        assertThat(disabledSender.sendEmail("servidor@fsph.se.gov.br", "Assunto", "Corpo")).isFalse();
    }

    private static User buildUser(String name, UserRole role) {
        Sector sector = new Sector("Setor " + name);
        ReflectionTestUtils.setField(sector, "id", UUID.randomUUID());
        User user = new User(sector, name, name.toLowerCase().replace(" ", ".") + "@fsph.se.gov.br", "hash", role);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private static Vehicle buildVehicle(String plate, String model, int capacity, boolean inMaintenance) {
        Sector sector = new Sector("Frota");
        ReflectionTestUtils.setField(sector, "id", UUID.randomUUID());
        Vehicle vehicle = new Vehicle(sector, plate, model, capacity, "Institucional", 15_000L);
        ReflectionTestUtils.setField(vehicle, "id", UUID.randomUUID());
        if (inMaintenance) {
            vehicle.startMaintenance();
        }
        return vehicle;
    }

    private static Driver buildDriver(String name) {
        User user = buildUser(name, UserRole.MOTORISTA);
        Driver driver = new Driver(user);
        ReflectionTestUtils.setField(driver, "id", UUID.randomUUID());
        return driver;
    }
}
