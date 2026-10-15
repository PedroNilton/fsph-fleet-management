package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.application.services.AuthService;
import br.com.gestaofrota.application.results.LoginResult;
import br.com.gestaofrota.api.v1.dtos.requests.LoginRequest;
import br.com.gestaofrota.api.v1.dtos.responses.LoginResponse;
import br.com.gestaofrota.api.v1.dtos.responses.CurrentUserResponse;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request.email(), request.password());
        LoginResponse response = new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn()
        );

        return ResponseEntity.ok(response);
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
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
