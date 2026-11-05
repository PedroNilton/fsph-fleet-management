package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.responses.FleetConsumptionReportResponse;
import br.com.gestaofrota.api.v1.errors.RestAccessDeniedHandler;
import br.com.gestaofrota.api.v1.errors.RestAuthenticationEntryPoint;
import br.com.gestaofrota.application.services.FuelingService;
import br.com.gestaofrota.application.services.PrimeFuelStationService;
import br.com.gestaofrota.domain.enums.FuelType;
import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.models.Fueling;
import br.com.gestaofrota.domain.models.PrimeFuelStation;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PrimeFuelStationController.class, FuelingController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class FuelingControllerAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrimeFuelStationService stations;

    @MockitoBean
    private FuelingService fuelings;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RevokedTokenService revokedTokenService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void enforcesRolePermissionsForMotoristAndFleetManager() throws Exception {
        UUID motoristId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        AuthenticatedUser motorist = new AuthenticatedUser(
                motoristId,
                UUID.randomUUID(),
                "Motorista Teste",
                "motorista@exemplo.com",
                "hash",
                UserRole.MOTORISTA,
                true,
                Set.of(Permission.FUEL_STATIONS_READ, Permission.FUELINGS_WRITE)
        );

        AuthenticatedUser manager = new AuthenticatedUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Gestor Teste",
                "gestor@exemplo.com",
                "hash",
                UserRole.GESTOR_FROTA,
                true,
                Set.of(
                        Permission.FUEL_STATIONS_READ,
                        Permission.FUEL_STATIONS_WRITE,
                        Permission.FUELINGS_READ,
                        Permission.FUELINGS_WRITE
                )
        );

        Vehicle vehicle = new Vehicle(new Sector("Frota"), "ABC1234", "Van", 8, "Transporte", 10_400L);
        ReflectionTestUtils.setField(vehicle, "id", vehicleId);
        PrimeFuelStation station = new PrimeFuelStation("Posto Prime", "12345678000190", "Aracaju", "SE", "Av. A, 1");
        ReflectionTestUtils.setField(station, "id", stationId);
        User userEntity = new User(new Sector("Frota"), "Motorista Teste", "motorista@exemplo.com", "hash", UserRole.MOTORISTA);
        ReflectionTestUtils.setField(userEntity, "id", motoristId);

        Fueling createdFueling = Fueling.register(
                vehicle,
                station,
                userEntity,
                LocalDate.of(2026, 10, 28),
                10_400L,
                10_000L,
                new BigDecimal("40.000"),
                new BigDecimal("240.00"),
                FuelType.GASOLINE,
                null
        );
        ReflectionTestUtils.setField(createdFueling, "id", UUID.randomUUID());

        when(stations.list(true)).thenReturn(List.of(station));
        when(fuelings.register(eq(motoristId), eq(vehicleId), eq(stationId), any(LocalDate.class), anyLong(),
                any(BigDecimal.class), any(BigDecimal.class), eq(FuelType.GASOLINE)))
                .thenReturn(createdFueling);
        when(fuelings.listMine(motoristId)).thenReturn(List.of(createdFueling));
        when(fuelings.listAnomalyAlerts(null)).thenReturn(List.of());
        when(fuelings.compareFleetConsumption()).thenReturn(new FleetConsumptionReportResponse(
                new BigDecimal("10.00"),
                new BigDecimal("40.000"),
                new BigDecimal("240.00"),
                1,
                0,
                List.of()
        ));

        mockMvc.perform(get("/api/v1/fuel-stations")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/fuel-stations").param("activeOnly", "true").with(user(motorist)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Posto Prime"));

        mockMvc.perform(post("/api/v1/fuel-stations")
                        .with(user(motorist))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Posto Novo",
                                  "cnpj": "12345678000190",
                                  "city": "Aracaju",
                                  "state": "SE",
                                  "address": "Rua Central, 10"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/fuelings")
                        .with(user(motorist))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "stationId": "%s",
                                  "fueledOn": "2026-10-28",
                                  "odometerKm": 10400,
                                  "liters": 40.0,
                                  "totalValue": 240.00,
                                  "fuelType": "GASOLINE"
                                }
                                """.formatted(vehicleId, stationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumptionKmPerLiter").value(10.00));

        mockMvc.perform(get("/api/v1/fuelings/mine").with(user(motorist)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/fuelings/alerts").with(user(motorist)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/fuelings/alerts").with(user(manager)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/fuelings/consumption").with(user(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fleetAverageKmPerLiter").value(10.00));
    }

    @Test
    void validatesStationAndFuelingPayloads() throws Exception {
        AuthenticatedUser manager = new AuthenticatedUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Gestor Teste",
                "gestor@exemplo.com",
                "hash",
                UserRole.GESTOR_FROTA,
                true,
                Set.of(Permission.FUEL_STATIONS_WRITE, Permission.FUELINGS_WRITE)
        );

        mockMvc.perform(post("/api/v1/fuel-stations")
                        .with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/fuelings")
                        .with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

