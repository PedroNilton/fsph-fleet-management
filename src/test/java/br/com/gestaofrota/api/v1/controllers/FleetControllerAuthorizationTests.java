package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.errors.RestAccessDeniedHandler;
import br.com.gestaofrota.api.v1.errors.RestAuthenticationEntryPoint;
import br.com.gestaofrota.application.services.DriverLicenseService;
import br.com.gestaofrota.application.services.DriverService;
import br.com.gestaofrota.application.services.VehicleDocumentService;
import br.com.gestaofrota.application.services.VehicleService;
import br.com.gestaofrota.infra.security.SecurityConfig;
import br.com.gestaofrota.infra.security.jwt.JwtService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({VehicleController.class, DriverController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class FleetControllerAuthorizationTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private VehicleService vehicles;
    @MockitoBean private VehicleDocumentService documents;
    @MockitoBean private DriverService drivers;
    @MockitoBean private DriverLicenseService licenses;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private RevokedTokenService revokedTokenService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void requiresAuthenticationAndFleetPermissions() throws Exception {
        when(vehicles.list()).thenReturn(List.of());
        when(drivers.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/vehicles")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/vehicles").with(user("motorista").authorities(() -> "VEHICLES_READ")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/drivers").with(user("motorista").authorities(() -> "VEHICLES_READ")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/drivers").with(user("gestor").authorities(() -> "DRIVERS_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void validatesVehicleAndDriverRequests() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .with(user("gestor").authorities(() -> "VEHICLES_WRITE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/drivers")
                        .with(user("gestor").authorities(() -> "DRIVERS_WRITE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
