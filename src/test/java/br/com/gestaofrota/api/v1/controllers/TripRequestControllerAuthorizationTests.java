package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.responses.FleetCalendarResponse;
import br.com.gestaofrota.api.v1.dtos.responses.TripRequestAvailabilityResponse;
import br.com.gestaofrota.api.v1.errors.RestAccessDeniedHandler;
import br.com.gestaofrota.api.v1.errors.RestAuthenticationEntryPoint;
import br.com.gestaofrota.application.services.NotificationService;
import br.com.gestaofrota.application.services.TripRequestService;
import br.com.gestaofrota.domain.enums.NotificationType;
import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.UserNotification;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.security.SecurityConfig;
import br.com.gestaofrota.infra.security.jwt.JwtService;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import br.com.gestaofrota.infra.security.services.CustomUserDetailsService;
import br.com.gestaofrota.infra.security.services.RevokedTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TripRequestController.class, NotificationController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class TripRequestControllerAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripRequestService tripRequests;

    @MockitoBean
    private NotificationService notifications;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RevokedTokenService revokedTokenService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void enforcesInstitutionalEmployeeMotoristAndFleetManagerPermissions() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        AuthenticatedUser employee = new AuthenticatedUser(
                employeeId,
                UUID.randomUUID(),
                "Servidor Institucional",
                "servidor@fsph.se.gov.br",
                "hash",
                UserRole.EMPLOYEE,
                true,
                Set.of(Permission.TRIP_REQUESTS_WRITE, Permission.NOTIFICATIONS_READ)
        );

        AuthenticatedUser motorist = new AuthenticatedUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Motorista FSPH",
                "motorista@fsph.se.gov.br",
                "hash",
                UserRole.MOTORISTA,
                true,
                Set.of(Permission.NOTIFICATIONS_READ)
        );

        AuthenticatedUser manager = new AuthenticatedUser(
                managerId,
                UUID.randomUUID(),
                "Gestor de Frota",
                "gestor@fsph.se.gov.br",
                "hash",
                UserRole.GESTOR_FROTA,
                true,
                Set.of(
                        Permission.TRIP_REQUESTS_READ,
                        Permission.TRIP_REQUESTS_WRITE,
                        Permission.TRIP_REQUESTS_MANAGE,
                        Permission.NOTIFICATIONS_READ
                )
        );

        Sector sector = new Sector("Vigilância");
        ReflectionTestUtils.setField(sector, "id", UUID.randomUUID());
        User requesterEntity = new User(sector, "Servidor Institucional", "servidor@fsph.se.gov.br", "hash", UserRole.EMPLOYEE);
        ReflectionTestUtils.setField(requesterEntity, "id", employeeId);
        User managerEntity = new User(sector, "Gestor de Frota", "gestor@fsph.se.gov.br", "hash", UserRole.GESTOR_FROTA);
        ReflectionTestUtils.setField(managerEntity, "id", managerId);

        Vehicle vehicle = new Vehicle(sector, "QKN1A23", "Spin", 7, "Institucional", 10_000L);
        ReflectionTestUtils.setField(vehicle, "id", vehicleId);

        User driverUser = new User(sector, "Motorista FSPH", "motorista@fsph.se.gov.br", "hash", UserRole.MOTORISTA);
        ReflectionTestUtils.setField(driverUser, "id", UUID.randomUUID());
        Driver driver = new Driver(driverUser);
        ReflectionTestUtils.setField(driver, "id", driverId);

        Instant departure = Instant.parse("2026-11-18T11:00:00Z");
        Instant expectedReturn = Instant.parse("2026-11-18T15:00:00Z");

        TripRequest sampleRequest = new TripRequest(
                requesterEntity,
                departure,
                expectedReturn,
                "Aracaju",
                "Lagarto",
                "Transporte de amostras",
                4,
                vehicle
        );
        ReflectionTestUtils.setField(sampleRequest, "id", requestId);

        when(tripRequests.listEligibleVehicles(anyInt(), any(), any())).thenReturn(List.of(vehicle));
        when(tripRequests.create(eq(employeeId), any(), any(), any(), any(), any(), eq(4), eq(vehicleId)))
                .thenReturn(sampleRequest);
        when(tripRequests.listMine(employeeId, null)).thenReturn(List.of(sampleRequest));

        mockMvc.perform(get("/api/v1/trip-requests/mine"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/trip-requests/eligible-vehicles")
                        .with(user(employee))
                        .param("passengers", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availablePassengerSeats").value(6));

        String createPayload = """
                {
                  "departureAt": "2026-11-18T11:00:00Z",
                  "expectedReturnAt": "2026-11-18T15:00:00Z",
                  "origin": "Aracaju",
                  "destination": "Lagarto",
                  "purpose": "Transporte de amostras",
                  "passengerCount": 4,
                  "requestedVehicleId": "%s"
                }
                """.formatted(vehicleId);

        mockMvc.perform(post("/api/v1/trip-requests")
                        .with(user(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.passengerCount").value(4));

        mockMvc.perform(post("/api/v1/trip-requests")
                        .with(user(motorist))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/trip-requests").with(user(employee)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/trip-requests/calendar")
                        .with(user(employee))
                        .param("from", "2026-11-18T00:00:00Z")
                        .param("to", "2026-11-19T00:00:00Z"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/trip-requests/{id}/approve", requestId).with(user(employee)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/trip-requests/{id}/schedule", requestId)
                        .with(user(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId": "%s", "driverId": "%s"}
                                """.formatted(vehicleId, driverId)))
                .andExpect(status().isForbidden());

        when(tripRequests.analyzeAvailability(requestId)).thenReturn(new TripRequestAvailabilityResponse(
                requestId, departure, expectedReturn, 4, vehicleId, true, List.of(), List.of()
        ));
        mockMvc.perform(get("/api/v1/trip-requests/{id}/availability", requestId).with(user(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedVehicleAvailable").value(true));

        mockMvc.perform(patch("/api/v1/trip-requests/{id}/reject", requestId)
                        .with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rejectionReason": "   "}
                                """))
                .andExpect(status().isBadRequest());

        sampleRequest.approve(managerEntity, vehicle, "Aprovado", Instant.now());
        when(tripRequests.approve(eq(requestId), eq(managerId), any(), any())).thenReturn(sampleRequest);

        mockMvc.perform(patch("/api/v1/trip-requests/{id}/approve", requestId).with(user(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        sampleRequest.schedule(managerEntity, vehicle, driver, Instant.now());
        when(tripRequests.schedule(requestId, managerId, vehicleId, driverId)).thenReturn(sampleRequest);

        mockMvc.perform(patch("/api/v1/trip-requests/{id}/schedule", requestId)
                        .with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId": "%s", "driverId": "%s"}
                                """.formatted(vehicleId, driverId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.assignedDriverId").value(driverId.toString()));

        when(tripRequests.getFleetCalendar(any(), any())).thenReturn(new FleetCalendarResponse(
                departure, expectedReturn, 1, 0, 1, 0, List.of(), List.of()
        ));
        mockMvc.perform(get("/api/v1/trip-requests/calendar")
                        .with(user(manager))
                        .param("from", "2026-11-18T00:00:00Z")
                        .param("to", "2026-11-19T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehiclesWithReservations").value(1));

        UserNotification notification = new UserNotification(
                requesterEntity,
                sampleRequest,
                NotificationType.TRIP_REQUEST_SCHEDULED,
                "Viagem agendada",
                "Sua viagem foi agendada."
        );
        ReflectionTestUtils.setField(notification, "id", notificationId);
        notification.markAsRead(Instant.now());

        when(notifications.countUnread(employeeId)).thenReturn(1L);
        when(notifications.markAsRead(notificationId, employeeId)).thenReturn(notification);

        mockMvc.perform(get("/api/v1/notifications/unread-count").with(user(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId).with(user(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }
}
