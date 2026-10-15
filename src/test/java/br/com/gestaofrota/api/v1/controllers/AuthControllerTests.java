package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.application.results.LoginResult;
import br.com.gestaofrota.application.services.AuthService;
import br.com.gestaofrota.infra.security.SecurityConfig;
import br.com.gestaofrota.api.v1.errors.RestAccessDeniedHandler;
import br.com.gestaofrota.api.v1.errors.RestAuthenticationEntryPoint;
import br.com.gestaofrota.infra.security.jwt.JwtUtil;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import br.com.gestaofrota.infra.security.services.CustomUserDetailsService;
import br.com.gestaofrota.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void returnsCurrentLoginContractForValidCredentials() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenReturn(new LoginResult("jwt-assinado", "Bearer", 900L));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@exemplo.com",
                                  "password": "senha"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "accessToken": "jwt-assinado",
                          "tokenType": "Bearer",
                          "expiresIn": 900
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void returnsBadRequestWithoutCallingServiceForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "email-invalido",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void returnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@exemplo.com",
                                  "password": "senha-incorreta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Falha na autenticação",
                          "status": 401,
                          "detail": "E-mail ou senha inválidos",
                          "instance": "/api/v1/auth/login"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void returnsAuthenticatedUserIdentity() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                userId,
                sectorId,
                "Gestora de Frota",
                "gestora@exemplo.com",
                "senha-codificada",
                UserRole.GESTOR_FROTA,
                true
        );

        mockMvc.perform(get("/api/v1/auth/me").with(user(authenticatedUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "id": "%s",
                          "sectorId": "%s",
                          "name": "Gestora de Frota",
                          "email": "gestora@exemplo.com",
                          "role": "GESTOR_FROTA"
                        }
                        """.formatted(userId, sectorId), JsonCompareMode.STRICT));
    }

    @Test
    void returnsProblemDetailWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Não autenticado",
                          "status": 401,
                          "detail": "Autenticação necessária",
                          "instance": "/api/v1/auth/me",
                          "code": "AUTHENTICATION_REQUIRED"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void returnsNoContentOnStatelessLogout() throws Exception {
        when(jwtUtil.extractSubject("jwt-valido")).thenReturn("admin@exemplo.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer jwt-valido")
                        .with(user("admin@exemplo.com").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verifyNoInteractions(authService);
    }
}
