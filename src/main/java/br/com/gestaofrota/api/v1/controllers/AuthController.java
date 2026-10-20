package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.application.services.AuthService;
import br.com.gestaofrota.api.v1.dtos.requests.LoginRequest;
import br.com.gestaofrota.api.v1.dtos.responses.LoginResponse;
import br.com.gestaofrota.api.v1.dtos.responses.CurrentUserResponse;
import br.com.gestaofrota.api.v1.dtos.LoginResult;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import br.com.gestaofrota.domain.exceptions.InvalidTokenException;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(new LoginResponse(result.accessToken(), "Bearer", result.expiresIn()));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(new CurrentUserResponse(
                user.id(),
                user.sectorId(),
                user.name(),
                user.email(),
                user.role()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String bearerPrefix = "Bearer ";
        if (authorization == null
                || !authorization.startsWith(bearerPrefix)
                || authorization.length() == bearerPrefix.length()) {
            throw new InvalidTokenException("Token ausente ou inválido");
        }
        authService.logout(authorization.substring(bearerPrefix.length()));
        return ResponseEntity.noContent().build();
    }
}
