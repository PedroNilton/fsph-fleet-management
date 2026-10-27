package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.infra.config.startup.AdminProperties;
import br.com.gestaofrota.PostgresIntegrationTest;
import br.com.gestaofrota.application.services.RolePermissionService;
import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
class LogoutIntegrationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminProperties adminProperties;

    @Autowired private RolePermissionService rolePermissionService;
    @Autowired private UserRepository userRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void logoutInvalidatesOnlyThePresentedToken() throws Exception {
        String firstToken = login();
        String secondToken = login();
        assertThat(secondToken).isNotEqualTo(firstToken);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk());
    }

    @Test
    void permissionUpdateTakesEffectForExistingTokenOnNextRequest() throws Exception {
        String adminToken = login();
        String email = "gestor-" + UUID.randomUUID() + "@example.com";
        Sector sector = sectorRepository.saveAndFlush(new Sector("Teste " + UUID.randomUUID()));
        userRepository.saveAndFlush(new User(sector, "Gestor", email,
                passwordEncoder.encode("senha-de-teste"), UserRole.GESTOR_FROTA));
        String gestorToken = login(email, "senha-de-teste");

        Set<Permission> originalPermissions = rolePermissionService.find(UserRole.GESTOR_FROTA);
        rolePermissionService.replace(UserRole.GESTOR_FROTA, Set.of(Permission.SECTORS_READ));
        try {
            mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permissions", containsInAnyOrder("SECTORS_READ")));

            mockMvc.perform(get("/api/v1/sectors").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestorToken))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            mockMvc.perform(put("/api/v1/roles/GESTOR_FROTA/permissions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"permissions\":[\"SECTORS_READ\",\"USERS_READ\"]}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestorToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permissions",
                            containsInAnyOrder("SECTORS_READ", "USERS_READ")));

            mockMvc.perform(put("/api/v1/roles/ADMIN/permissions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"permissions\":[]}"))
                    .andExpect(status().isConflict());
        } finally {
            rolePermissionService.replace(UserRole.GESTOR_FROTA, originalPermissions);
        }
    }

    private String login() throws Exception {
        return login(adminProperties.email(), adminProperties.password());
    }

    private String login(String email, String password) throws Exception {
        String credentials = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password
        ));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
